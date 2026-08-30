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

export KUBECONFIG="${KUBECONFIG:-$HOME/.kube/config}"
work_dir="$(mktemp -d)"

cleanup() {
  rm -rf -- "$work_dir"
  rm -f -- "$archive"
}

diagnostics() {
  set +e
  echo "::group::identity-governance deployment diagnostics"
  helm --namespace "$k8s_namespace" status segroup8
  helm --namespace "$k8s_namespace" history segroup8
  kubectl --namespace "$k8s_namespace" get pods,service,ingress -o wide
  kubectl --namespace "$k8s_namespace" describe deployment/segroup8-identity-governance
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-identity-governance --all-containers --tail=200
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

kubectl get namespace "$k8s_namespace" >/dev/null
kubectl --namespace "$k8s_namespace" get secret acr-pull-secret >/dev/null
kubectl --namespace "$k8s_namespace" get secret identity-governance-secret >/dev/null

# The service upgrades one component of the existing shared release. Reusing
# values preserves the images and configuration owned by the other services.
helm --namespace "$k8s_namespace" status segroup8 >/dev/null
build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
helm upgrade --install segroup8 "$chart_dir" \
  --namespace "$k8s_namespace" \
  --reuse-values \
  --atomic \
  --cleanup-on-fail \
  --wait \
  --timeout 10m \
  --history-max 5 \
  --set identityGovernance.enabled=true \
  --set-string "identityGovernance.image.repository=$registry/$registry_namespace/identity-governance" \
  --set-string "identityGovernance.image.tag=$image_tag" \
  --set-string "identityGovernance.deployment.version=$image_tag" \
  --set-string "identityGovernance.deployment.commit=$release_id" \
  --set-string "identityGovernance.deployment.buildTime=$build_time"

kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-identity-governance --timeout=5m
identity_info="$(kubectl --namespace "$k8s_namespace" exec deployment/segroup8-identity-governance -- \
  curl --fail --silent --show-error http://127.0.0.1:8091/actuator/info)"
grep -F '"version":"'"$image_tag"'"' <<<"$identity_info" >/dev/null
grep -F '"commit":"'"$release_id"'"' <<<"$identity_info" >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-identity-governance -- \
  curl --fail --silent --show-error http://127.0.0.1:8091/actuator/health/liveness >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-identity-governance -- \
  curl --fail --silent --show-error http://127.0.0.1:8091/actuator/health/readiness >/dev/null
identity_smoke="$(kubectl --namespace "$k8s_namespace" exec deployment/segroup8-identity-governance -- \
  curl --fail --silent --show-error -H 'Content-Type: application/json' \
  -d '{"username":"__deployment_smoke__","password":"invalid"}' \
  http://127.0.0.1:8091/api/auth/login)"
grep -F '"code":401' <<<"$identity_smoke" >/dev/null

echo "identity-governance-service deployed successfully as $image_tag"
