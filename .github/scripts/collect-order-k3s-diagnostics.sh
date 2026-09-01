#!/usr/bin/env bash
set -Eeuo pipefail
export KUBECONFIG="$HOME/.kube/config"

k8s_namespace="${1:-segroup8}"
helm_release="${2:-segroup8}"
diagnostics_dir="$(mktemp -d)"

cleanup() {
  rm -rf -- "$diagnostics_dir"
}
trap cleanup EXIT

capture() {
  local output_name="$1"
  shift
  "$@" >"$diagnostics_dir/$output_name.txt" 2>&1 || true
}

printf '{"collectedAt":"%s","namespace":"%s","release":"%s"}\n' \
  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$k8s_namespace" "$helm_release" \
  >"$diagnostics_dir/metadata.json"

capture helm-status helm --namespace "$k8s_namespace" status "$helm_release"
capture helm-history helm --namespace "$k8s_namespace" history "$helm_release"
capture workloads kubectl --namespace "$k8s_namespace" get deployment,pods,service,endpoints,ingress -o wide
capture order-deployment kubectl --namespace "$k8s_namespace" describe deployment/segroup8-order
capture namespace-events kubectl --namespace "$k8s_namespace" get events --sort-by=.metadata.creationTimestamp
capture order-logs kubectl --namespace "$k8s_namespace" logs deployment/segroup8-order --all-containers --tail=500 --timestamps
capture order-previous-logs kubectl --namespace "$k8s_namespace" logs deployment/segroup8-order --all-containers --previous --tail=500 --timestamps
capture order-manifest kubectl --namespace "$k8s_namespace" get deployment/segroup8-order -o yaml

tar -C "$diagnostics_dir" -czf - .
