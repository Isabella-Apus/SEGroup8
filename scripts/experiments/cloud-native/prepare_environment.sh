#!/usr/bin/env bash
set -euo pipefail

RUN_ID="${1:-$(date +%Y%m%d-%H%M%S)}"
NAMESPACE="segroup8-cloud-exp-${RUN_ID,,}"
NAMESPACE="${NAMESPACE//_/-}"
HOST_ROOT="${2:-/root/segroup8-experiments/$RUN_ID}"
BASE_IMAGE="${BASE_IMAGE:-crpi-oyyvfqoq4lnnoc8v.cn-hangzhou.personal.cr.aliyuncs.com/segroup8/backend:sha-bb72290cff96c78ab189468b82db1f8ba3cd9323}"
MYSQL_IMAGE="${MYSQL_IMAGE:-crpi-oyyvfqoq4lnnoc8v.cn-hangzhou.personal.cr.aliyuncs.com/segroup8/mysql:8.4.6}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
EVIDENCE="$HOST_ROOT/evidence"

if [[ ! "$NAMESPACE" =~ ^segroup8-cloud-exp-[a-z0-9-]+$ ]]; then
  echo "Unsafe namespace: $NAMESPACE" >&2
  exit 2
fi
for artifact in \
  "$HOST_ROOT/jars/identity-governance-service-1.0.0.jar" \
  "$HOST_ROOT/jars/secondhand-service-1.0.0.jar" \
  "$HOST_ROOT/jars/order-service-1.0.0.jar"; do
  test -f "$artifact" || { echo "Missing artifact: $artifact" >&2; exit 3; }
done
if kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
  echo "Namespace already exists: $NAMESPACE" >&2
  exit 4
fi

mkdir -p "$HOST_ROOT/mysql-init" "$EVIDENCE/environment"
MYSQL_ROOT_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_hex(24))')"
DB_PASSWORD="$(python3 -c 'import secrets; print(secrets.token_hex(24))')"
JWT_SECRET="$(python3 -c 'import secrets; print(secrets.token_hex(40))')"
INTERNAL_SERVICE_TOKEN="$(python3 -c 'import secrets; print(secrets.token_hex(32))')"

sed "s/__DB_PASSWORD__/$DB_PASSWORD/g" \
  "$SCRIPT_DIR/sql/00-create-databases.sql.tpl" > "$HOST_ROOT/mysql-init/00-create-databases.sql"
{
  echo 'USE monolith_db;'
  cat "$REPO_ROOT/backend/src/main/resources/schema.sql"
} > "$HOST_ROOT/mysql-init/01-monolith-schema.sql"
{
  echo 'USE monolith_db;'
  cat "$REPO_ROOT/docker/mysql/02-seed.sql"
} > "$HOST_ROOT/mysql-init/02-monolith-course-seed.sql"
cp "$SCRIPT_DIR/sql/03-seed-monolith-performance.sql" "$HOST_ROOT/mysql-init/03-seed-monolith-performance.sql"

SECONDHAND_SHA="$(sha256sum "$HOST_ROOT/jars/secondhand-service-1.0.0.jar" | awk '{print $1}')"
ORDER_SHA="$(sha256sum "$HOST_ROOT/jars/order-service-1.0.0.jar" | awk '{print $1}')"
IDENTITY_SHA="$(sha256sum "$HOST_ROOT/jars/identity-governance-service-1.0.0.jar" | awk '{print $1}')"
GIT_COMMIT="${GIT_COMMIT:-$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || echo unknown)}"
MONOLITH_DIGEST="$(k3s ctr images list | awk -v image="$BASE_IMAGE" '$1==image {digest=$3} END {print digest}')"

RENDERED="$HOST_ROOT/experiment-stack.rendered.yaml"
sed \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__HOST_ROOT__|$HOST_ROOT|g" \
  -e "s|__BASE_IMAGE__|$BASE_IMAGE|g" \
  -e "s|__MYSQL_IMAGE__|$MYSQL_IMAGE|g" \
  -e "s|__RUN_ID__|$RUN_ID|g" \
  -e "s|__GIT_COMMIT__|$GIT_COMMIT|g" \
  -e "s|__SECONDHAND_JAR_SHA256__|$SECONDHAND_SHA|g" \
  -e "s|__ORDER_JAR_SHA256__|$ORDER_SHA|g" \
  -e "s|__IDENTITY_JAR_SHA256__|$IDENTITY_SHA|g" \
  -e "s|__MONOLITH_IMAGE_DIGEST__|$MONOLITH_DIGEST|g" \
  -e "s|__MYSQL_ROOT_PASSWORD__|$MYSQL_ROOT_PASSWORD|g" \
  -e "s|__DB_PASSWORD__|$DB_PASSWORD|g" \
  -e "s|__JWT_SECRET__|$JWT_SECRET|g" \
  -e "s|__INTERNAL_SERVICE_TOKEN__|$INTERNAL_SERVICE_TOKEN|g" \
  "$SCRIPT_DIR/experiment-stack.yaml.tpl" > "$RENDERED"
chmod 600 "$RENDERED"

kubectl apply -f "$RENDERED"
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/monolith --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/identity-governance-service --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/order-service --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=300s

MYSQL_POD="$(kubectl -n "$NAMESPACE" get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"
kubectl -n "$NAMESPACE" exec -i "$MYSQL_POD" -- \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < "$SCRIPT_DIR/sql/04-seed-secondhand-performance.sql"
kubectl -n "$NAMESPACE" exec -i "$MYSQL_POD" -- \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < "$SCRIPT_DIR/sql/05-seed-identity-fault.sql"

cat > "$HOST_ROOT/state.env" <<STATE
RUN_ID=$RUN_ID
NAMESPACE=$NAMESPACE
HOST_ROOT=$HOST_ROOT
BASE_IMAGE=$BASE_IMAGE
MYSQL_IMAGE=$MYSQL_IMAGE
GIT_COMMIT=$GIT_COMMIT
SECONDHAND_JAR_SHA256=$SECONDHAND_SHA
ORDER_JAR_SHA256=$ORDER_SHA
IDENTITY_JAR_SHA256=$IDENTITY_SHA
MONOLITH_IMAGE_DIGEST=$MONOLITH_DIGEST
STATE

{
  date --iso-8601=seconds
  uname -a
  lscpu
  free -h
  kubectl version
  kubectl top nodes
  kubectl -n "$NAMESPACE" get deployment,service,hpa,pod -o wide
  kubectl -n "$NAMESPACE" top pods
  echo "monolith_count=$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" monolith_db -e 'SELECT COUNT(*) FROM secondhand_product WHERE id BETWEEN 900001 AND 900500')"
  echo "microservice_count=$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e 'SELECT COUNT(*) FROM secondhand_product WHERE id BETWEEN 900001 AND 900500')"
} > "$EVIDENCE/environment/inventory.txt" 2>&1
kubectl -n "$NAMESPACE" get deployment,service,hpa -o yaml > "$EVIDENCE/environment/kubernetes-resources.yaml"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$EVIDENCE/environment/events.txt"
cp "$HOST_ROOT/state.env" "$EVIDENCE/environment/artifact-metadata.env"

echo "Prepared namespace $NAMESPACE"
echo "State file: $HOST_ROOT/state.env"
