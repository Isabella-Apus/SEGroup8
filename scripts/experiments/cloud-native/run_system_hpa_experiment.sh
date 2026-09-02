#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
OUT="${1:-/root/segroup8-experiments/system-hpa-$RUN_ID}"
NAMESPACE="${NAMESPACE:-segroup8}"
HELM_RELEASE="${HELM_RELEASE:-segroup8}"
DEPLOYMENT="${DEPLOYMENT:-segroup8-backend}"
HPA_NAME="${HPA_NAME:-segroup8-backend}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-http://127.0.0.1}"
LOAD_BASE_URL="${LOAD_BASE_URL:-}"
MIN_REPLICAS="${MIN_REPLICAS:-2}"
MAX_REPLICAS="${MAX_REPLICAS:-4}"
TARGET_CPU="${TARGET_CPU:-60}"
CONCURRENCIES="${CONCURRENCIES:-10 20 40}"
STAGE_DURATION="${STAGE_DURATION:-45}"
WARMUP_DURATION="${WARMUP_DURATION:-30}"
SCALE_TRIGGER_DURATION="${SCALE_TRIGGER_DURATION:-120}"
REQUEST_TIMEOUT="${REQUEST_TIMEOUT:-15}"
SCALE_DOWN_TIMEOUT="${SCALE_DOWN_TIMEOUT:-300}"
KEEP_OPTIMIZED_HPA="${KEEP_OPTIMIZED_HPA:-true}"
CLEANUP_SEED="${CLEANUP_SEED:-true}"
ENDPOINT_FILE="${ENDPOINT_FILE:-$SCRIPT_DIR/system-hpa-endpoints.txt}"
MYSQL_STATEFULSET="${MYSQL_STATEFULSET:-mysql}"
MYSQL_SECRET="${MYSQL_SECRET:-segroup8-mysql-secret}"

case "$NAMESPACE" in
  ''|*[!a-z0-9-]*) echo "Unsafe namespace: $NAMESPACE" >&2; exit 2 ;;
esac
case "$DEPLOYMENT" in
  ''|*[!a-zA-Z0-9-]*) echo "Unsafe deployment: $DEPLOYMENT" >&2; exit 2 ;;
esac
if [[ "$DEPLOYMENT" == *secondhand* || "$HPA_NAME" == *secondhand* ]]; then
  echo "The course HPA experiment must target the complete-system backend, not secondhand-service" >&2
  exit 2
fi
case "$HELM_RELEASE" in
  ''|*[!a-zA-Z0-9-]*) echo "Unsafe Helm release: $HELM_RELEASE" >&2; exit 2 ;;
esac
for value in "$MIN_REPLICAS" "$MAX_REPLICAS" "$TARGET_CPU"; do
  [[ "$value" =~ ^[0-9]+$ ]] || { echo "Replica and CPU values must be integers" >&2; exit 2; }
done
(( MIN_REPLICAS >= 2 && MAX_REPLICAS >= MIN_REPLICAS )) || {
  echo "Expected 2 <= MIN_REPLICAS <= MAX_REPLICAS" >&2
  exit 2
}

mkdir -p "$OUT/raw" "$OUT/logs" "$OUT/environment" "$OUT/database"
touch "$OUT/NOT_RUN_OR_FAILURES.txt"
ORIGINAL_REPLICAS="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.spec.replicas}')"
ORIGINAL_HPA_PRESENT=false
if kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" >/dev/null 2>&1; then
  ORIGINAL_HPA_PRESENT=true
  kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o json | python3 -c \
    'import json,sys; x=json.load(sys.stdin); x.pop("status",None); m=x["metadata"]; [m.pop(k,None) for k in ("uid","resourceVersion","creationTimestamp","generation","managedFields")]; print(json.dumps(x))' \
    > "$OUT/environment/original-hpa.json"
fi
INDEX_VISIBLE=true
SUCCESS=false

mysql_password() {
  kubectl -n "$NAMESPACE" get secret "$MYSQL_SECRET" -o jsonpath='{.data.MYSQL_ROOT_PASSWORD}' | base64 -d
}

mysql_database() {
  kubectl -n "$NAMESPACE" get secret "$MYSQL_SECRET" -o jsonpath='{.data.MYSQL_DATABASE}' | base64 -d
}

mysql_run() {
  local password database
  password="$(mysql_password)"
  database="$(mysql_database)"
  kubectl -n "$NAMESPACE" exec -i "$MYSQL_STATEFULSET-0" -- \
    env MYSQL_PWD="$password" mysql -uroot "$database" "$@"
}

collect_diagnostics() {
  set +e
  kubectl -n "$NAMESPACE" get deployment,pod,service,ingress,hpa -o wide > "$OUT/environment/workloads-final.txt" 2>&1
  kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUT/environment/events.txt" 2>&1
  kubectl -n "$NAMESPACE" describe hpa "$HPA_NAME" > "$OUT/hpa-describe.txt" 2>&1
  kubectl -n "$NAMESPACE" logs deployment/"$DEPLOYMENT" --all-containers --tail=300 > "$OUT/logs/backend.log" 2>&1
  kubectl -n "$NAMESPACE" logs deployment/segroup8-frontend --all-containers --tail=300 > "$OUT/logs/frontend.log" 2>&1
  kubectl -n "$NAMESPACE" logs "$MYSQL_STATEFULSET-0" --tail=300 > "$OUT/logs/mysql.log" 2>&1
  set -e
}

restore_original() {
  set +e
  if ! $INDEX_VISIBLE; then
    mysql_run -e 'ALTER TABLE secondhand_product ALTER INDEX idx_secondhand_status_created VISIBLE' >/dev/null 2>&1
    INDEX_VISIBLE=true
  fi
  if [[ "$CLEANUP_SEED" == true ]]; then
    mysql_run -e 'DELETE FROM secondhand_product WHERE id BETWEEN 9800001 AND 9805000' >/dev/null 2>&1
  fi
  if ! $SUCCESS || [[ "$KEEP_OPTIMIZED_HPA" != true ]]; then
    kubectl -n "$NAMESPACE" delete hpa "$HPA_NAME" --ignore-not-found >/dev/null 2>&1
    if $ORIGINAL_HPA_PRESENT; then
      kubectl apply -f "$OUT/environment/original-hpa.json" >/dev/null 2>&1
    else
      kubectl -n "$NAMESPACE" scale deployment "$DEPLOYMENT" --replicas="$ORIGINAL_REPLICAS" >/dev/null 2>&1
    fi
  fi
  set -e
}

on_exit() {
  local status=$?
  [[ $status -eq 0 ]] || echo "EXPERIMENT_EXIT_CODE=$status" >> "$OUT/NOT_RUN_OR_FAILURES.txt"
  collect_diagnostics
  restore_original
  exit "$status"
}
trap on_exit EXIT

wait_ready() {
  local expected="$1" deadline=$((SECONDS + 240))
  while (( SECONDS < deadline )); do
    local available
    available="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.status.availableReplicas}')"
    [[ "${available:-0}" -ge "$expected" ]] && return 0
    sleep 5
  done
  echo "Timed out waiting for $expected ready backend replicas" >&2
  return 1
}

snapshot() {
  local mode="$1" stage="$2" now current desired ready cpu
  now="$(date --iso-8601=seconds)"
  current="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.status.replicas}')"
  ready="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.status.readyReplicas}')"
  if kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" >/dev/null 2>&1; then
    desired="$(kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o jsonpath='{.status.desiredReplicas}')"
    cpu="$(kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o jsonpath='{.status.currentMetrics[0].resource.current.averageUtilization}' 2>/dev/null || true)"
  else
    desired="$MIN_REPLICAS"
    cpu="NA"
  fi
  echo "$now,$mode,$stage,${current:-0},${desired:-0},${ready:-0},${cpu:-NA}" >> "$OUT/replica-timeline.csv"
  {
    echo "[$now] mode=$mode stage=$stage"
    kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o wide 2>/dev/null || true
    kubectl -n "$NAMESPACE" top pod -l app.kubernetes.io/component=backend
    kubectl top node
  } >> "$OUT/resource-snapshots.log" 2>&1
}

run_benchmark() {
  local mode="$1" concurrency="$2" stage="c$concurrency"
  python3 "$SCRIPT_DIR/http_mix_benchmark.py" \
    --name "$mode-$stage" --base-url "$LOAD_BASE_URL" --endpoint-file "$ENDPOINT_FILE" \
    --concurrency "$concurrency" --duration "$STAGE_DURATION" --timeout "$REQUEST_TIMEOUT" \
    --output "$OUT/raw/$mode-$stage.json" > "$OUT/raw/$mode-$stage.console.log" 2>&1 &
  local pid=$!
  while kill -0 "$pid" 2>/dev/null; do
    snapshot "$mode" "$stage"
    sleep 5
  done
  wait "$pid"
  snapshot "$mode" "$stage-post"
}

warm_up() {
  local mode="$1" label="${2:-warmup}" duration="${3:-$WARMUP_DURATION}"
  python3 "$SCRIPT_DIR/http_mix_benchmark.py" \
    --name "$mode-$label" --base-url "$LOAD_BASE_URL" --endpoint-file "$ENDPOINT_FILE" \
    --concurrency 4 --duration "$duration" --timeout "$REQUEST_TIMEOUT" \
    --output "$OUT/raw/$mode-$label.json" > "$OUT/raw/$mode-$label.console.log" 2>&1 &
  local pid=$!
  while kill -0 "$pid" 2>/dev/null; do
    snapshot "$mode" "$label"
    sleep 5
  done
  wait "$pid"
  snapshot "$mode" "$label-post"
}

wait_hpa_scale_ready() {
  local deadline=$((SECONDS + 240)) desired ready
  while (( SECONDS < deadline )); do
    snapshot hpa scale-ready
    desired="$(kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o jsonpath='{.status.desiredReplicas}')"
    ready="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.status.readyReplicas}')"
    [[ "${desired:-0}" -gt "$MIN_REPLICAS" && "${ready:-0}" -ge "${desired:-0}" ]] && return 0
    sleep 5
  done
  echo 'HPA_SCALE_OUT_PODS_NEVER_BECAME_READY' >> "$OUT/NOT_RUN_OR_FAILURES.txt"
  return 1
}

for command in kubectl python3 curl base64 sed; do command -v "$command" >/dev/null; done
kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes >/dev/null
kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" >/dev/null
kubectl -n "$NAMESPACE" get statefulset "$MYSQL_STATEFULSET" >/dev/null
curl --fail --silent --show-error "$PUBLIC_BASE_URL/health" >/dev/null
if [[ -z "$LOAD_BASE_URL" ]]; then
  FRONTEND_SERVICE_IP="$(kubectl -n "$NAMESPACE" get service frontend -o jsonpath='{.spec.clusterIP}')"
  LOAD_BASE_URL="http://$FRONTEND_SERVICE_IP"
fi
while read -r weight endpoint; do
  [[ -z "${weight:-}" || "$weight" == \#* ]] && continue
  curl --fail --silent --show-error "$LOAD_BASE_URL$endpoint" >/dev/null
done < "$ENDPOINT_FILE"

{
  date --iso-8601=seconds
  uname -a
  kubectl version
  kubectl get nodes -o wide
  kubectl top nodes
  kubectl -n "$NAMESPACE" get deployment,pod,service,ingress,hpa -o wide
  kubectl -n "$NAMESPACE" top pods
  echo "publicBaseUrl=$PUBLIC_BASE_URL"
  echo "loadBaseUrl=$LOAD_BASE_URL"
  echo "loadPath=frontend Service/Nginx -> shared compatibility backend -> MySQL"
  echo "autoscalingTarget=deployment/$DEPLOYMENT"
  echo "excludedAutoscalingTarget=secondhand-service"
  echo "originalBackendReplicas=$ORIGINAL_REPLICAS"
  echo "backendImage=$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.spec.template.spec.containers[0].image}')"
  echo "frontendImage=$(kubectl -n "$NAMESPACE" get deployment segroup8-frontend -o jsonpath='{.spec.template.spec.containers[0].image}')"
  kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='backendRequests={.spec.template.spec.containers[0].resources.requests}{"\n"}backendLimits={.spec.template.spec.containers[0].resources.limits}{"\n"}'
} > "$OUT/environment/inventory.txt" 2>&1
kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" segroup8-frontend -o yaml > "$OUT/environment/deployments.yaml"
kubectl -n "$NAMESPACE" get ingress,service -o yaml > "$OUT/environment/routing.yaml"
cp "$ENDPOINT_FILE" "$OUT/environment/endpoints.txt"
BACKEND_SERVICE_IP="$(kubectl -n "$NAMESPACE" get service backend -o jsonpath='{.spec.clusterIP}')"
curl --fail --silent --show-error "http://$BACKEND_SERVICE_IP:8080/actuator/health/liveness" > "$OUT/environment/backend-liveness.json"
curl --fail --silent --show-error "http://$BACKEND_SERVICE_IP:8080/actuator/health/readiness" > "$OUT/environment/backend-readiness.json"
curl --fail --silent --show-error "http://$BACKEND_SERVICE_IP:8080/actuator/info" > "$OUT/environment/backend-version.json"
curl --fail --silent --show-error "$PUBLIC_BASE_URL/health" > "$OUT/environment/frontend-health.txt"

mysql_run < "$SCRIPT_DIR/sql/06-seed-system-hpa.sql"
mysql_run -e "SELECT COUNT(*) AS temporary_rows FROM secondhand_product WHERE id BETWEEN 9800001 AND 9805000" > "$OUT/database/seed-count.txt"
mysql_run -e "SET @exists=(SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='secondhand_product' AND INDEX_NAME='idx_secondhand_status_created'); SET @ddl=IF(@exists=0,'CREATE INDEX idx_secondhand_status_created ON secondhand_product (status, create_time DESC, id)','SELECT 1'); PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;"
mysql_run -e 'ALTER TABLE secondhand_product ALTER INDEX idx_secondhand_status_created INVISIBLE'
INDEX_VISIBLE=false
mysql_run -e 'EXPLAIN ANALYZE SELECT id, seller_user_id, name, sale_price, status, create_time FROM secondhand_product WHERE status=1 ORDER BY create_time DESC LIMIT 100' > "$OUT/database/explain-before.txt"
mysql_run -e 'ALTER TABLE secondhand_product ALTER INDEX idx_secondhand_status_created VISIBLE'
INDEX_VISIBLE=true
mysql_run -e 'EXPLAIN ANALYZE SELECT id, seller_user_id, name, sale_price, status, create_time FROM secondhand_product WHERE status=1 ORDER BY create_time DESC LIMIT 100' > "$OUT/database/explain-after.txt"
mysql_run -e "SHOW INDEX FROM secondhand_product WHERE Key_name='idx_secondhand_status_created'" > "$OUT/database/index.txt"
grep -q 'Table scan on secondhand_product' "$OUT/database/explain-before.txt" || {
  echo 'SLOW_QUERY_BASELINE_TABLE_SCAN_NOT_OBSERVED' >> "$OUT/NOT_RUN_OR_FAILURES.txt"
  exit 1
}
grep -q 'idx_secondhand_status_created' "$OUT/database/explain-after.txt" || {
  echo 'OPTIMIZED_QUERY_DID_NOT_USE_COMPOSITE_INDEX' >> "$OUT/NOT_RUN_OR_FAILURES.txt"
  exit 1
}
python3 - "$OUT/database" <<'PY'
import json, os, re, sys
root = sys.argv[1]
def finish_time(name):
    text = open(os.path.join(root, name), encoding="utf-8").read()
    match = re.search(r"actual time=[0-9.]+\.\.([0-9.]+)", text)
    if not match:
        raise SystemExit(f"cannot parse actual time from {name}")
    return float(match.group(1)), text
before_ms, before = finish_time("explain-before.txt")
after_ms, after = finish_time("explain-after.txt")
summary = {
    "query": "secondhand public list: status=1 ORDER BY create_time DESC LIMIT 100",
    "temporaryRows": 5000,
    "before": {"executionMs": before_ms, "plan": "table-scan-and-sort"},
    "after": {"executionMs": after_ms, "plan": "idx_secondhand_status_created"},
    "executionTimeReductionPercent": (before_ms - after_ms) / before_ms * 100 if before_ms else None,
}
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump(summary, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
PY

sed -e "s/__NAMESPACE__/$NAMESPACE/g" \
  -e "s/__HELM_RELEASE__/$HELM_RELEASE/g" \
  -e "s/__DEPLOYMENT__/$DEPLOYMENT/g" \
  -e "s/__HPA_NAME__/$HPA_NAME/g" \
  -e "s/__MIN_REPLICAS__/$MIN_REPLICAS/g" \
  -e "s/__MAX_REPLICAS__/$MAX_REPLICAS/g" \
  -e "s/__TARGET_CPU__/$TARGET_CPU/g" \
  "$SCRIPT_DIR/system-backend-hpa.yaml.tpl" > "$OUT/environment/system-backend-hpa.yaml"

echo 'timestamp,mode,stage,currentReplicas,desiredReplicas,readyReplicas,cpuAverageUtilizationPercent' > "$OUT/replica-timeline.csv"
: > "$OUT/resource-snapshots.log"

# Fixed two-replica baseline. Scaling to two first prevents a transient drop when an old HPA exists.
kubectl -n "$NAMESPACE" scale deployment "$DEPLOYMENT" --replicas="$MIN_REPLICAS" >/dev/null
kubectl -n "$NAMESPACE" delete hpa "$HPA_NAME" --ignore-not-found >/dev/null
wait_ready "$MIN_REPLICAS"
warm_up fixed warmup "$WARMUP_DURATION"
for concurrency in $CONCURRENCIES; do run_benchmark fixed "$concurrency"; done

# Optimized system configuration: two warm replicas absorb bursts while HPA adds capacity.
kubectl apply -f "$OUT/environment/system-backend-hpa.yaml" >/dev/null
wait_ready "$MIN_REPLICAS"
warm_up hpa scale-trigger "$SCALE_TRIGGER_DURATION"
wait_hpa_scale_ready
warm_up hpa post-scale-warmup "$WARMUP_DURATION"
for concurrency in $CONCURRENCIES; do run_benchmark hpa "$concurrency"; done

deadline=$((SECONDS + SCALE_DOWN_TIMEOUT))
while (( SECONDS < deadline )); do
  snapshot hpa scale-down
  current="$(kubectl -n "$NAMESPACE" get hpa "$HPA_NAME" -o jsonpath='{.status.currentReplicas}')"
  ready="$(kubectl -n "$NAMESPACE" get deployment "$DEPLOYMENT" -o jsonpath='{.status.readyReplicas}')"
  [[ "${current:-0}" == "$MIN_REPLICAS" && "${ready:-0}" == "$MIN_REPLICAS" ]] && break
  sleep 10
done

python3 - "$OUT" "$MIN_REPLICAS" <<'PY'
import csv, glob, json, os, statistics, sys

root, minimum = sys.argv[1], int(sys.argv[2])
with open(os.path.join(root, "replica-timeline.csv"), encoding="utf-8") as stream:
    timeline = list(csv.DictReader(stream))
runs = []
for path in sorted(glob.glob(os.path.join(root, "raw", "*.json"))):
    if "warmup" in os.path.basename(path) or "scale-trigger" in os.path.basename(path):
        continue
    with open(path, encoding="utf-8") as stream:
        item = json.load(stream)
    runs.append({
        "name": item["name"],
        "concurrency": item["concurrency"],
        "throughput": item["throughputRequestsPerSecond"],
        "averageMs": item["latencyMs"]["average"],
        "p95Ms": item["latencyMs"]["p95"],
        "errorRate": item["errorRate"],
        "requests": item["requests"],
    })
hpa_points = [point for point in timeline if point["mode"] == "hpa"]
peak = max((int(point["currentReplicas"]) for point in hpa_points), default=0)
peak_ready = max((int(point["readyReplicas"]) for point in hpa_points), default=0)
final = int(hpa_points[-1]["currentReplicas"]) if hpa_points else 0
summary = {
    "scope": "complete-system",
    "trafficPath": "frontend Service/Nginx -> shared compatibility backend -> MySQL",
    "autoscalingTarget": "shared compatibility backend (never secondhand-service)",
    "runs": runs,
    "hpa": {
        "minimumReplicas": minimum,
        "peakReplicas": peak,
        "peakReadyReplicas": peak_ready,
        "finalReplicas": final,
        "scaleOutObserved": peak > minimum,
        "usableScaleOutObserved": peak_ready > minimum,
        "scaleInObserved": final == minimum,
    },
    "maximumErrorRate": max((run["errorRate"] for run in runs), default=1),
}
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump(summary, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
failures = []
if not summary["hpa"]["scaleOutObserved"]:
    failures.append("HPA_SCALE_OUT_NOT_OBSERVED")
if not summary["hpa"]["usableScaleOutObserved"]:
    failures.append("HPA_SCALE_OUT_PODS_NEVER_BECAME_READY")
if not summary["hpa"]["scaleInObserved"]:
    failures.append("HPA_SCALE_IN_NOT_OBSERVED")
if summary["maximumErrorRate"] > 0.05:
    failures.append(f"ERROR_RATE_ABOVE_5_PERCENT={summary['maximumErrorRate']:.6f}")
if failures:
    with open(os.path.join(root, "NOT_RUN_OR_FAILURES.txt"), "a", encoding="utf-8") as stream:
        stream.write("\n".join(failures) + "\n")
    raise SystemExit("; ".join(failures))
PY

SUCCESS=true
echo "Complete-system HPA experiment passed: $OUT"
if [[ "$KEEP_OPTIMIZED_HPA" == true ]]; then
  echo "Optimized HPA remains enabled with minReplicas=$MIN_REPLICAS."
else
  echo "The pre-experiment replica/HPA state will be restored by the exit handler."
fi
