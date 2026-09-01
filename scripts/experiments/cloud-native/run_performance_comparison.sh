#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="${1:?usage: run_performance_comparison.sh /path/to/state.env}"
source "$STATE_FILE"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$HOST_ROOT/evidence/performance"
mkdir -p "$OUT/raw" "$OUT/resources"

# Keep the comparison one replica versus one replica. HPA is enabled again by
# run_hpa_experiment.sh, where scaling is the variable under test.
kubectl -n "$NAMESPACE" delete hpa secondhand-service --ignore-not-found
kubectl -n "$NAMESPACE" scale deployment/monolith deployment/secondhand-service --replicas=1
kubectl -n "$NAMESPACE" rollout status deployment/monolith --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=180s

MONOLITH_IP="$(kubectl -n "$NAMESPACE" get service monolith -o jsonpath='{.spec.clusterIP}')"
SECONDHAND_IP="$(kubectl -n "$NAMESPACE" get service secondhand-service -o jsonpath='{.spec.clusterIP}')"
CONCURRENCY="${CONCURRENCY:-20}"
DURATION="${DURATION:-30}"
WARMUP="${WARMUP:-5}"

run_one() {
  local system="$1" endpoint="$2" run="$3" url="$4" app_label="$5"
  local stem="${endpoint}-${system}-run${run}"
  if [[ -s "$OUT/raw/$stem.json" ]]; then
    echo "resume: keeping completed result $stem"
    return
  fi
  kubectl -n "$NAMESPACE" wait --for=condition=available "deployment/$app_label" --timeout=180s >/dev/null
  local ready_url="${url%%/api/*}/actuator/health/readiness"
  for _ in $(seq 1 36); do
    curl -fsS "$ready_url" >/dev/null 2>&1 && break
    sleep 5
  done
  curl -fsS "$ready_url" >/dev/null
  python3 "$SCRIPT_DIR/http_benchmark.py" \
    --name "$stem" --url "$url" --concurrency "$CONCURRENCY" \
    --duration "$DURATION" --warmup "$WARMUP" --output "$OUT/raw/$stem.json" \
    > "$OUT/raw/$stem.console.log" 2>&1 &
  local benchmark_pid=$!
  {
    echo "timestamp,pod,cpu,memory"
    while kill -0 "$benchmark_pid" 2>/dev/null; do
      kubectl -n "$NAMESPACE" top pod -l "app=$app_label" --no-headers 2>/dev/null | \
        awk -v ts="$(date --iso-8601=seconds)" '{print ts "," $1 "," $2 "," $3}'
      sleep 2
    done
  } > "$OUT/resources/$stem.csv"
  wait "$benchmark_pid"
  sleep 5
}

for run in 1 2 3; do
  if (( run % 2 == 1 )); then systems=(monolith microservice); else systems=(microservice monolith); fi
  for endpoint in list keyword detail; do
    for system in "${systems[@]}"; do
      if [[ "$system" == monolith ]]; then
        base="http://$MONOLITH_IP:8080/api/secondhand"
        label=monolith
      else
        base="http://$SECONDHAND_IP:8080/api/secondhand"
        label=secondhand-service
      fi
      case "$endpoint" in
        list) url="$base/list?pageNum=1&pageSize=50" ;;
        keyword) url="$base/list?keyword=Experiment&pageNum=1&pageSize=20&sortBy=priceAsc" ;;
        detail) url="$base/detail/900001" ;;
      esac
      run_one "$system" "$endpoint" "$run" "$url" "$label"
    done
  done
done

python3 - "$OUT" <<'PY'
import glob, json, os, statistics, sys
root = sys.argv[1]
rows = []
for path in sorted(glob.glob(os.path.join(root, "raw", "*.json"))):
    with open(path, encoding="utf-8") as stream:
        item = json.load(stream)
    endpoint, system, run = item["name"].rsplit("-", 2)
    rows.append({
        "endpoint": endpoint, "system": system, "run": int(run[3:]),
        "throughput": item["throughputRequestsPerSecond"],
        "averageMs": item["latencyMs"]["average"], "p95Ms": item["latencyMs"]["p95"],
        "errorRate": item["errorRate"], "requests": item["requests"],
    })
groups = {}
for row in rows:
    groups.setdefault((row["endpoint"], row["system"]), []).append(row)
aggregates = []
for (endpoint, system), values in sorted(groups.items()):
    throughputs = [v["throughput"] for v in values]
    averages = [v["averageMs"] for v in values if v["averageMs"] is not None]
    p95s = [v["p95Ms"] for v in values if v["p95Ms"] is not None]
    aggregates.append({
        "endpoint": endpoint, "system": system, "runs": len(values),
        "runsWithSuccessfulLatency": len(averages),
        "medianThroughput": statistics.median(throughputs),
        "medianAverageMs": statistics.median(averages) if averages else None,
        "medianP95Ms": statistics.median(p95s) if p95s else None,
        "totalRequests": sum(v["requests"] for v in values),
        "weightedErrorRate": sum(v["errorRate"] * v["requests"] for v in values) / sum(v["requests"] for v in values),
    })
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump({"runs": rows, "aggregates": aggregates}, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
PY

kubectl -n "$NAMESPACE" get pod -o wide > "$OUT/final-pods.txt"
kubectl -n "$NAMESPACE" top pod > "$OUT/final-top.txt"
kubectl -n "$NAMESPACE" describe pod -l app=monolith > "$OUT/monolith-pod-describe.txt"
kubectl -n "$NAMESPACE" describe pod -l app=secondhand-service > "$OUT/secondhand-pod-describe.txt"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUT/events.txt"
echo "Performance evidence: $OUT"
