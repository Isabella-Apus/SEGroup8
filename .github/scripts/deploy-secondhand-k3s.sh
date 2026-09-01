#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?release id is required}"
archive="${2:?chart archive is required}"
registry="${3:?ACR registry is required}"
registry_namespace="${4:?ACR namespace is required}"
image_tag="${5:?image tag is required}"
k8s_namespace="${6:-segroup8}"

if [[ ! "$release_id" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Release id must be a full Git commit SHA" >&2
  exit 2
fi

command -v helm >/dev/null
command -v kubectl >/dev/null
command -v curl >/dev/null
command -v flock >/dev/null

export KUBECONFIG="$HOME/.kube/config"
exec 9>"$HOME/.segroup8-helm.lock"
flock -w 3300 9 || { echo "Timed out waiting for the shared Helm deployment lock" >&2; exit 1; }
work_dir="$(mktemp -d)"

cleanup() {
  rm -rf -- "$work_dir"
  rm -f -- "$archive"
}

diagnostics() {
  set +e
  echo "::group::secondhand-service deployment diagnostics"
  helm --namespace "$k8s_namespace" status segroup8
  helm --namespace "$k8s_namespace" history segroup8
  kubectl --namespace "$k8s_namespace" get pods,service,ingress -o wide
  kubectl --namespace "$k8s_namespace" describe deployment/segroup8-secondhand
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-secondhand --all-containers --tail=200
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-secondhand --all-containers --previous --tail=200
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
kubectl --namespace "$k8s_namespace" get secret segroup8-secondhand-secret >/dev/null
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
  --set secondhand.enabled=true \
  --set secondhand.autoscaling.enabled=true \
  --set-string "secondhand.image.repository=$registry/$registry_namespace/secondhand" \
  --set-string "secondhand.image.tag=$image_tag" \
  --set-string "secondhand.deployment.version=$image_tag" \
  --set-string "secondhand.deployment.commit=$release_id" \
  --set-string "secondhand.deployment.buildTime=$build_time"

kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-secondhand --timeout=5m
secondhand_info="$(kubectl --namespace "$k8s_namespace" exec deployment/segroup8-secondhand -- \
  curl --fail --silent --show-error http://127.0.0.1:8080/actuator/info)"
grep -F '"version":"'"$image_tag"'"' <<<"$secondhand_info" >/dev/null
grep -F '"commit":"'"$release_id"'"' <<<"$secondhand_info" >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-secondhand -- \
  curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/liveness >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-secondhand -- \
  curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness >/dev/null
secondhand_smoke="$(kubectl --namespace "$k8s_namespace" exec deployment/segroup8-secondhand -- \
  curl --fail --silent --show-error http://127.0.0.1:8080/api/secondhand/list)"
grep -F '"code":0' <<<"$secondhand_smoke" >/dev/null

echo "secondhand-service deployed successfully as $image_tag"
