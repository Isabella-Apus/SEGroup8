#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="${1:?usage: run_hpa_experiment.sh /path/to/state.env}"
source "$STATE_FILE"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$HOST_ROOT/evidence/hpa"
mkdir -p "$OUT/raw"
ROUNDS="${ROUNDS:-3}"
CONCURRENCY="${CONCURRENCY:-80}"
DURATION="${DURATION:-90}"
WARMUP="${WARMUP:-5}"
SCALE_DOWN_TIMEOUT="${SCALE_DOWN_TIMEOUT:-300}"

# Re-applying the redacted-at-rest runtime manifest recreates the exact v2 HPA
# definition (1..4 replicas, 60% CPU, explicit up/down behavior).
kubectl apply -f "$HOST_ROOT/experiment-stack.rendered.yaml" >/dev/null
kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=180s
SECONDHAND_IP="$(kubectl -n "$NAMESPACE" get service secondhand-service -o jsonpath='{.spec.clusterIP}')"
URL="http://$SECONDHAND_IP:8080/api/secondhand/list?pageNum=1&pageSize=100&keyword=Experiment"

snapshot() {
  local round="$1" phase="$2"
  local now current desired cpu ready
  now="$(date --iso-8601=seconds)"
  current="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.currentReplicas}')"
  desired="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.desiredReplicas}')"
  cpu="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.currentMetrics[0].resource.current.averageUtilization}' 2>/dev/null || true)"
  ready="$(kubectl -n "$NAMESPACE" get deployment secondhand-service -o jsonpath='{.status.readyReplicas}')"
  echo "$now,$round,$phase,${current:-0},${desired:-0},${ready:-0},${cpu:-NA}" >> "$OUT/replica-timeline.csv"
  {
    echo "[$now] round=$round phase=$phase"
    kubectl -n "$NAMESPACE" get hpa secondhand-service -o wide
    kubectl -n "$NAMESPACE" top pod -l app=secondhand-service
    kubectl top node
  } >> "$OUT/resource-snapshots.log" 2>&1
}

echo "timestamp,round,phase,currentReplicas,desiredReplicas,readyReplicas,cpuAverageUtilizationPercent" > "$OUT/replica-timeline.csv"
: > "$OUT/resource-snapshots.log"

for round in $(seq 1 "$ROUNDS"); do
  kubectl -n "$NAMESPACE" scale deployment secondhand-service --replicas=1 >/dev/null
  kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=180s >/dev/null
  for _ in $(seq 1 24); do
    replicas="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.currentReplicas}')"
    [[ "${replicas:-0}" == 1 ]] && break
    sleep 5
  done
  snapshot "$round" pre-load

  python3 "$SCRIPT_DIR/http_benchmark.py" \
    --name "hpa-round-$round" --url "$URL" --concurrency "$CONCURRENCY" \
    --duration "$DURATION" --warmup "$WARMUP" --output "$OUT/raw/hpa-round-$round.json" \
    > "$OUT/raw/hpa-round-$round.console.log" 2>&1 &
  benchmark_pid=$!
  while kill -0 "$benchmark_pid" 2>/dev/null; do
    snapshot "$round" load
    sleep 5
  done
  wait "$benchmark_pid"
  snapshot "$round" post-load

  deadline=$((SECONDS + SCALE_DOWN_TIMEOUT))
  while (( SECONDS < deadline )); do
    snapshot "$round" scale-down
    current="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.currentReplicas}')"
    ready="$(kubectl -n "$NAMESPACE" get deployment secondhand-service -o jsonpath='{.status.readyReplicas}')"
    if [[ "${current:-0}" == 1 && "${ready:-0}" == 1 ]]; then
      break
    fi
    sleep 10
  done
  final="$(kubectl -n "$NAMESPACE" get hpa secondhand-service -o jsonpath='{.status.currentReplicas}')"
  [[ "${final:-0}" == 1 ]] || echo "ROUND_$round SCALE_DOWN_TIMEOUT" >> "$OUT/NOT_RUN_OR_FAILURES.txt"
done

python3 - "$OUT" <<'PY'
import csv, glob, json, os, sys
root = sys.argv[1]
with open(os.path.join(root, "replica-timeline.csv"), encoding="utf-8") as stream:
    timeline = list(csv.DictReader(stream))
runs = []
for path in sorted(glob.glob(os.path.join(root, "raw", "hpa-round-*.json"))):
    with open(path, encoding="utf-8") as stream:
        item = json.load(stream)
    round_no = int(item["name"].rsplit("-", 1)[1])
    points = [p for p in timeline if int(p["round"]) == round_no]
    runs.append({
        "round": round_no,
        "initialReplicas": int(points[0]["currentReplicas"]),
        "peakReplicas": max(int(p["currentReplicas"]) for p in points),
        "finalReplicas": int(points[-1]["currentReplicas"]),
        "scaleOutObserved": max(int(p["currentReplicas"]) for p in points) > 1,
        "scaleInObserved": int(points[-1]["currentReplicas"]) == 1,
        "throughput": item["throughputRequestsPerSecond"],
        "averageMs": item["latencyMs"]["average"],
        "p95Ms": item["latencyMs"]["p95"],
        "errorRate": item["errorRate"],
        "requests": item["requests"],
    })
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump({"runs": runs}, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
PY

kubectl -n "$NAMESPACE" describe hpa secondhand-service > "$OUT/hpa-describe.txt"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUT/events.txt"
echo "HPA evidence: $OUT"
