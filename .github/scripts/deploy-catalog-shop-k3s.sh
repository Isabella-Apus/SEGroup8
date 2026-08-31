#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?release id is required}"
archive="${2:?chart archive is required}"
registry="${3:?ACR registry is required}"
registry_namespace="${4:?ACR namespace is required}"
image_tag="${5:?image tag is required}"
k8s_namespace="${6:-segroup8}"

[[ "$release_id" =~ ^[0-9a-f]{40}$ ]] || { echo "Release id must be a full Git commit SHA" >&2; exit 2; }
command -v helm >/dev/null
command -v kubectl >/dev/null
command -v curl >/dev/null
export KUBECONFIG="${KUBECONFIG:-$HOME/.kube/config}"
work_dir="$(mktemp -d)"

cleanup() { rm -rf -- "$work_dir"; rm -f -- "$archive"; }
diagnostics() {
  set +e
  echo "::group::catalog-shop-service deployment diagnostics"
  helm --namespace "$k8s_namespace" status segroup8
  helm --namespace "$k8s_namespace" history segroup8
  kubectl --namespace "$k8s_namespace" get pods,service,ingress,hpa -o wide
  kubectl --namespace "$k8s_namespace" describe deployment/segroup8-catalog-shop
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-catalog-shop --all-containers --tail=200
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-catalog-shop --all-containers --previous --tail=200
  echo "::endgroup::"
  set -e
}
on_exit() { status=$?; [[ $status -eq 0 ]] || diagnostics; cleanup; }
trap on_exit EXIT

tar -xzf "$archive" -C "$work_dir"
chart_dir="$work_dir/deploy/helm/segroup8"
test -f "$chart_dir/Chart.yaml"
kubectl get namespace "$k8s_namespace" >/dev/null
kubectl --namespace "$k8s_namespace" get secret acr-pull-secret >/dev/null
kubectl --namespace "$k8s_namespace" get secret segroup8-catalog-shop-secret >/dev/null
helm --namespace "$k8s_namespace" status segroup8 >/dev/null

helm upgrade --install segroup8 "$chart_dir" \
  --namespace "$k8s_namespace" --reset-then-reuse-values --atomic --cleanup-on-fail --wait \
  --timeout 10m --history-max 5 \
  --set catalogShop.enabled=true \
  --set-string "catalogShop.image.repository=$registry/$registry_namespace/catalog-shop" \
  --set-string "catalogShop.image.tag=$image_tag" \
  --set-string "catalogShop.deployment.version=$image_tag" \
  --set-string "catalogShop.deployment.commit=$release_id" \
  --set-string "catalogShop.deployment.buildTime=$(date -u +%Y-%m-%dT%H:%M:%SZ)"

kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-catalog-shop --timeout=5m
service_info="$(kubectl --namespace "$k8s_namespace" exec deployment/segroup8-catalog-shop -- curl --fail --silent --show-error http://127.0.0.1:8080/actuator/info)"
grep -F '"version":"'"$image_tag"'"' <<<"$service_info" >/dev/null
grep -F '"commit":"'"$release_id"'"' <<<"$service_info" >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-catalog-shop -- curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/liveness >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-catalog-shop -- curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness >/dev/null
kubectl --namespace "$k8s_namespace" exec deployment/segroup8-catalog-shop -- curl --fail --silent --show-error http://127.0.0.1:8080/api/category/tree >/dev/null
echo "catalog-shop-service deployed successfully as $image_tag"
