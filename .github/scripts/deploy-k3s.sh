#!/usr/bin/env bash
set -Eeuo pipefail

release_id="${1:?release id is required}"
archive="${2:?release archive is required}"
registry="${3:?ACR registry is required}"
registry_namespace="${4:?ACR namespace is required}"
image_tag="${5:?image tag is required}"
k8s_namespace="${6:-segroup8}"

if [[ ! "$release_id" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Release id must be a full Git commit SHA" >&2
  exit 2
fi

if [[ ! "$registry" =~ ^[a-zA-Z0-9.-]+(:[0-9]+)?$ ]]; then
  echo "ACR registry must be a hostname with an optional port" >&2
  exit 2
fi

if [[ ! "$registry_namespace" =~ ^[a-zA-Z0-9._-]+$ ]]; then
  echo "ACR namespace contains invalid characters" >&2
  exit 2
fi

if [[ ! "$k8s_namespace" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]]; then
  echo "Kubernetes namespace is invalid" >&2
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
trap cleanup EXIT

tar -xzf "$archive" -C "$work_dir"
chart_dir="$work_dir/deploy/helm/segroup8"
schema_file="$work_dir/backend/src/main/resources/schema.sql"
test -f "$chart_dir/Chart.yaml"
test -s "$schema_file"

kubectl get namespace "$k8s_namespace" >/dev/null
for secret_name in acr-pull-secret segroup8-backend-secret segroup8-mysql-secret; do
  kubectl --namespace "$k8s_namespace" get secret "$secret_name" >/dev/null
done

build_time="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

helm upgrade --install segroup8 "$chart_dir" \
  --namespace "$k8s_namespace" \
  --cleanup-on-fail \
  --wait \
  --timeout 10m \
  --history-max 5 \
  --set-string "backend.image.repository=$registry/$registry_namespace/backend" \
  --set-string "backend.image.tag=$image_tag" \
  --set-string "frontend.image.repository=$registry/$registry_namespace/frontend" \
  --set-string "frontend.image.tag=$image_tag" \
  --set-string "mysql.image.repository=$registry/$registry_namespace/mysql" \
  --set-string "deployment.version=$image_tag" \
  --set-string "deployment.commit=$release_id" \
  --set-string "deployment.buildTime=$build_time" \
  --set-file "mysql.initSchema=$schema_file"

kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-backend --timeout=5m
kubectl --namespace "$k8s_namespace" rollout status deployment/segroup8-frontend --timeout=5m
kubectl --namespace "$k8s_namespace" get pods,service,ingress

# K3s installs Traefik by default, so this verifies the public routing path from
# the host without exposing the backend actuator endpoint through the Ingress.
curl --fail --silent --show-error \
  --retry 12 --retry-all-errors --retry-delay 5 \
  http://127.0.0.1/health >/dev/null

echo "Release $release_id deployed successfully as $image_tag"
