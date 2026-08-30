#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="${1:?usage: cleanup_environment.sh /path/to/state.env}"
source "$STATE_FILE"
if [[ ! "$NAMESPACE" =~ ^segroup8-cloud-exp-[a-z0-9-]+$ ]]; then
  echo "Refusing to delete unsafe namespace: $NAMESPACE" >&2
  exit 2
fi

kubectl get namespace "$NAMESPACE" -o yaml > "$HOST_ROOT/evidence/environment/namespace-before-cleanup.yaml"
kubectl delete namespace "$NAMESPACE" --wait=true --timeout=300s
date --iso-8601=seconds > "$HOST_ROOT/evidence/environment/cleanup-completed-at.txt"

# Evidence and non-secret reproduction sources remain on the host. Only the
# rendered manifest and generated SQL containing ephemeral credentials are removed.
for secret_file in \
  "$HOST_ROOT/experiment-stack.rendered.yaml" \
  "$HOST_ROOT/mysql-init/00-create-databases.sql"; do
  if [[ -f "$secret_file" ]]; then
    shred -u "$secret_file"
  fi
done
echo "Deleted isolated namespace $NAMESPACE; retained evidence under $HOST_ROOT/evidence"
