#!/usr/bin/env bash
set -Eeuo pipefail

: "${DEPLOY_HOST:?DEPLOY_HOST is required}"
: "${DEPLOY_USER:?DEPLOY_USER is required}"
: "${ACR_REGISTRY:?ACR_REGISTRY is required}"
: "${ACR_USERNAME:?ACR_USERNAME is required}"
: "${ACR_PASSWORD:?ACR_PASSWORD is required}"
: "${K8S_NAMESPACE:?K8S_NAMESPACE is required}"

auth_b64=""
docker_config_b64=""
cleanup() {
  unset auth_b64 docker_config_b64 ACR_PASSWORD
}
trap cleanup EXIT

if [[ ! "$ACR_REGISTRY" =~ ^[a-zA-Z0-9.-]+(:[0-9]+)?$ ]]; then
  echo "ACR_REGISTRY must be a hostname with an optional port" >&2
  exit 2
fi

if [[ ! "$K8S_NAMESPACE" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]]; then
  echo "K8S_NAMESPACE is invalid" >&2
  exit 2
fi

auth_b64="$(printf '%s:%s' "$ACR_USERNAME" "$ACR_PASSWORD" | base64 -w0)"
docker_config_b64="$(
  printf '{"auths":{"%s":{"auth":"%s"}}}' "$ACR_REGISTRY" "$auth_b64" |
    base64 -w0
)"

cat <<EOF | ssh \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=5 \
  "$DEPLOY_USER@$DEPLOY_HOST" \
  "kubectl --namespace '$K8S_NAMESPACE' apply -f - >/dev/null"
apiVersion: v1
kind: Secret
metadata:
  name: acr-pull-secret
  namespace: $K8S_NAMESPACE
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: $docker_config_b64
EOF

ssh \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=5 \
  "$DEPLOY_USER@$DEPLOY_HOST" \
  "kubectl --namespace '$K8S_NAMESPACE' get secret acr-pull-secret -o jsonpath='{.data.\\.dockerconfigjson}' | base64 -d | grep -Fq '\"$ACR_REGISTRY\"'"

echo "acr-pull-secret synchronized for $ACR_REGISTRY"
