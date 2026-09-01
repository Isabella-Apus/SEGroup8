#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?release id is required}"
archive="${2:?chart archive is required}"
registry="${3:?ACR registry is required}"
registry_namespace="${4:?ACR namespace is required}"
image_tag="${5:?image tag is required}"
k8s_namespace="${6:-segroup8}"
realtime_allowed_origins="${7:?REALTIME_ALLOWED_ORIGIN_PATTERNS is required}"
identity_service_url="${8:-}"

if [[ ! "$release_id" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Release id must be a full Git commit SHA" >&2
  exit 2
fi

if [[ -z "$realtime_allowed_origins" || "$realtime_allowed_origins" == "*" ]]; then
  echo "REALTIME_ALLOWED_ORIGIN_PATTERNS must be an explicit non-wildcard allow-list" >&2
  exit 2
fi

command -v helm >/dev/null
command -v kubectl >/dev/null
command -v wget >/dev/null

export KUBECONFIG="$HOME/.kube/config"
work_dir="$(mktemp -d)"

cleanup() {
  rm -rf -- "$work_dir"
  rm -f -- "$archive"
}

diagnostics() {
  set +e
  echo "::group::messaging-service deployment diagnostics"
  helm --namespace "$k8s_namespace" status segroup8
  helm --namespace "$k8s_namespace" history segroup8
  kubectl --namespace "$k8s_namespace" get pods,service,ingress -o wide
  kubectl --namespace "$k8s_namespace" describe deployment/messaging
  kubectl --namespace "$k8s_namespace" logs deployment/messaging --all-containers --tail=200
  kubectl --namespace "$k8s_namespace" logs deployment/messaging --all-containers --previous --tail=200
  echo "::endgroup::"
  set -e
}

on_exit() {
  status=$?
  if [[ $status -ne 0 ]]; then
    diagnostics
  fi
  cleanup
}
trap on_exit EXIT

tar -xzf "$archive" -C "$work_dir"
chart_dir="$work_dir/deploy/helm/segroup8"
test -f "$chart_dir/Chart.yaml"

kubectl --namespace "$k8s_namespace" get secret acr-pull-secret >/dev/null
kubectl --namespace "$k8s_namespace" get secret segroup8-messaging-secret >/dev/null
helm --namespace "$k8s_namespace" status segroup8 >/dev/null

build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
helm upgrade --install segroup8 "$chart_dir" \
  --namespace "$k8s_namespace" \
  --reset-then-reuse-values \
  --atomic \
  --cleanup-on-fail \
  --wait \
  --timeout 10m \
  --history-max 5 \
  --set messaging.enabled=true \
  --set-string "messaging.image.repository=$registry/$registry_namespace/messaging" \
  --set-string "messaging.image.tag=$image_tag" \
  --set-string "messaging.config.realtimeAllowedOriginPatterns=$realtime_allowed_origins" \
  --set-string "messaging.config.identityServiceUrl=$identity_service_url" \
  --set-string "messaging.deployment.version=$image_tag" \
  --set-string "messaging.deployment.commit=$release_id" \
  --set-string "messaging.deployment.buildTime=$build_time"

kubectl --namespace "$k8s_namespace" rollout status deployment/messaging --timeout=5m
messaging_info="$(kubectl --namespace "$k8s_namespace" exec deployment/messaging -- \
  wget -qO- http://127.0.0.1:8084/actuator/info)"
grep -F '"version":"'"$image_tag"'"' <<<"$messaging_info" >/dev/null
grep -F '"commit":"'"$release_id"'"' <<<"$messaging_info" >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/messaging -- \
  wget -qO- http://127.0.0.1:8084/actuator/health/liveness >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/messaging -- \
  wget -qO- http://127.0.0.1:8084/actuator/health/readiness >/dev/null

echo "messaging-service deployed successfully as $image_tag"
