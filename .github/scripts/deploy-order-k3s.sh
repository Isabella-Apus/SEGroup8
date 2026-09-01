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
command -v flock >/dev/null
export KUBECONFIG="$HOME/.kube/config"
exec 9>"$HOME/.segroup8-helm.lock"
flock -w 3300 9 || { echo "Timed out waiting for the shared Helm deployment lock" >&2; exit 1; }
work_dir="$(mktemp -d)"

cleanup() { rm -rf -- "$work_dir"; rm -f -- "$archive"; }
diagnostics() {
  set +e
  echo "::group::order-service deployment diagnostics"
  helm --namespace "$k8s_namespace" status segroup8
  helm --namespace "$k8s_namespace" history segroup8
  kubectl --namespace "$k8s_namespace" get pods,service,ingress -o wide
  kubectl --namespace "$k8s_namespace" describe deployment/segroup8-order
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-order --all-containers --tail=200
  kubectl --namespace "$k8s_namespace" logs deployment/segroup8-order --all-containers --previous --tail=200
  echo "::endgroup::"
  set -e
}
on_exit() { status=$?; [[ $status -eq 0 ]] || diagnostics; cleanup; }
trap on_exit EXIT

tar -xzf "$archive" -C "$work_dir"
chart_dir="$work_dir/deploy/helm/segroup8"
test -f "$chart_dir/Chart.yaml"
kubectl --namespace "$k8s_namespace" get secret acr-pull-secret >/dev/null
kubectl --namespace "$k8s_namespace" get secret segroup8-order-secret >/dev/null
helm --namespace "$k8s_namespace" status segroup8 >/dev/null

build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
helm upgrade --install segroup8 "$chart_dir" \
  --namespace "$k8s_namespace" --reset-then-reuse-values --atomic --cleanup-on-fail --wait \
  --timeout 10m --history-max 5 \
  --set order.enabled=true \
  --set-string "order.image.repository=$registry/$registry_namespace/order" \
  --set-string "order.image.tag=$image_tag" \
  --set-string "order.deployment.version=$image_tag" \
  --set-string "order.deployment.commit=$release_id" \
  --set-string "order.deployment.buildTime=$build_time"

kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-order --timeout=5m
service_ip="$(kubectl --namespace "$k8s_namespace" get service segroup8-order -o jsonpath='{.spec.clusterIP}')"
[[ -n "$service_ip" && "$service_ip" != "None" ]]
service_url="http://${service_ip}:8085"
order_info="$(curl --fail --silent --show-error "$service_url/actuator/info")"
grep -F '"version":"'"$image_tag"'"' <<<"$order_info" >/dev/null
grep -F '"commit":"'"$release_id"'"' <<<"$order_info" >/dev/null
curl --fail --silent --show-error "$service_url/actuator/health/liveness" >/dev/null
curl --fail --silent --show-error "$service_url/actuator/health/readiness" >/dev/null
status="$(curl --silent --output /dev/null --write-out '%{http_code}' "$service_url/api/order/list")"
[[ "$status" == "401" ]]
echo "order-service deployed successfully as $image_tag"
