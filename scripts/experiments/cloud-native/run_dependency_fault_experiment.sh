#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="${1:?usage: run_dependency_fault_experiment.sh /path/to/state.env}"
source "$STATE_FILE"
OUT="$HOST_ROOT/evidence/dependency-fault"
mkdir -p "$OUT"
MYSQL_POD="$(kubectl -n "$NAMESPACE" get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"
MYSQL_ROOT_PASSWORD="$(kubectl -n "$NAMESPACE" get secret experiment-secrets -o jsonpath='{.data.MYSQL_ROOT_PASSWORD}' | base64 -d)"
JWT_SECRET="$(kubectl -n "$NAMESPACE" get secret experiment-secrets -o jsonpath='{.data.JWT_SECRET}' | base64 -d)"
SECONDHAND_IP="$(kubectl -n "$NAMESPACE" get service secondhand-service -o jsonpath='{.spec.clusterIP}')"
BASE_URL="http://$SECONDHAND_IP:8080"

# Reset only the dedicated fault specimen. The 499 remaining performance rows
# stay unchanged, so this drill cannot invalidate the comparison dataset.
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
  "SET FOREIGN_KEY_CHECKS=0; DELETE FROM secondhand_db.outbox_event WHERE aggregate_id LIKE '900500%'; DELETE FROM secondhand_db.trade_order_request WHERE product_id=900500; UPDATE secondhand_db.secondhand_product SET status=1,risk_status='APPROVED',version=0,deleted=0 WHERE id=900500; SET FOREIGN_KEY_CHECKS=1; DELETE FROM order_db.order_item WHERE product_id=900500; DELETE FROM order_db.order_info WHERE business_key LIKE '%900500%';"

TOKEN="$(JWT_SECRET="$JWT_SECRET" python3 - <<'PY'
import base64, hashlib, hmac, json, os, time
def enc(value):
    raw = json.dumps(value, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()
head = enc({"alg":"HS256","typ":"JWT"})
body = enc({"uid":3,"username":"experiment-buyer","role":"USER","iat":int(time.time()),"exp":int(time.time())+3600})
sig = base64.urlsafe_b64encode(hmac.new(os.environ["JWT_SECRET"].encode(), f"{head}.{body}".encode(), hashlib.sha256).digest()).rstrip(b"=").decode()
print(f"{head}.{body}.{sig}")
PY
)"

kubectl -n "$NAMESPACE" delete hpa secondhand-service --ignore-not-found >/dev/null
kubectl -n "$NAMESPACE" scale deployment secondhand-service --replicas=1 >/dev/null
kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=180s >/dev/null
kubectl -n "$NAMESPACE" get deployment,pod -o wide > "$OUT/00-before-fault.txt"

FAULT_START="$(date --iso-8601=seconds)"
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=0 > "$OUT/01-fault-injection.txt"
kubectl -n "$NAMESPACE" wait --for=delete pod -l app=order-service --timeout=120s >> "$OUT/01-fault-injection.txt" 2>&1 || true
kubectl -n "$NAMESPACE" get deployment,pod -o wide >> "$OUT/01-fault-injection.txt"

HTTP_CODE="$(curl -sS -o "$OUT/02-buy-during-outage-response.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/secondhand/900500/buy" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data '{"addressId":1,"remark":"dependency-fault-experiment"}')"
echo "$HTTP_CODE" > "$OUT/02-buy-during-outage-status.txt"
curl -sS "$BASE_URL/actuator/health/liveness" > "$OUT/03-liveness-during-outage.json"
curl -sS "$BASE_URL/actuator/health/readiness" > "$OUT/04-readiness-during-outage.json"
curl -sS "$BASE_URL/api/secondhand/list?pageNum=1&pageSize=5&keyword=Experiment" > "$OUT/05-unrelated-list-during-outage.json"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e \
  "SELECT id,trade_type,trade_id,order_business_key,product_id,request_status,attempts,last_error,next_retry_at FROM trade_order_request WHERE product_id=900500;" \
  > "$OUT/06-request-state-during-outage.tsv"
kubectl -n "$NAMESPACE" logs deployment/secondhand-service --since-time="$FAULT_START" --timestamps > "$OUT/07-secondhand-outage.log" 2>&1

RECOVERY_START="$(date --iso-8601=seconds)"
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=1 > "$OUT/08-recovery.txt"
kubectl -n "$NAMESPACE" rollout status deployment/order-service --timeout=180s >> "$OUT/08-recovery.txt"
deadline=$((SECONDS + 180))
state=""
while (( SECONDS < deadline )); do
  state="$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e "SELECT request_status FROM trade_order_request WHERE product_id=900500 ORDER BY id DESC LIMIT 1;")"
  echo "$(date --iso-8601=seconds) $state" >> "$OUT/09-recovery-timeline.txt"
  [[ "$state" == CREATED ]] && break
  sleep 2
done

kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e \
  "SELECT id,order_business_key,request_status,attempts,last_error,order_id,order_no,order_status FROM trade_order_request WHERE product_id=900500; SELECT id,status,version FROM secondhand_product WHERE id=900500;" \
  > "$OUT/10-recovered-secondhand-state.tsv"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT id,order_no,business_key,order_status FROM order_info WHERE business_key LIKE '%900500%'; SELECT COUNT(*) AS matching_order_count FROM order_info WHERE business_key LIKE '%900500%';" \
  > "$OUT/11-recovered-order-state.tsv"

REPEAT_CODE="$(curl -sS -o "$OUT/12-repeat-buy-response.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/secondhand/900500/buy" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data '{"addressId":1,"remark":"dependency-fault-experiment-repeat"}')"
echo "$REPEAT_CODE" > "$OUT/12-repeat-buy-status.txt"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT COUNT(*) FROM order_info WHERE business_key LIKE '%900500%';" > "$OUT/13-order-count-after-repeat.txt"
kubectl -n "$NAMESPACE" logs deployment/secondhand-service --since-time="$RECOVERY_START" --timestamps > "$OUT/14-secondhand-recovery.log" 2>&1
kubectl -n "$NAMESPACE" logs deployment/order-service --since-time="$RECOVERY_START" --timestamps > "$OUT/15-order-recovery.log" 2>&1
kubectl -n "$NAMESPACE" get deployment,pod -o wide > "$OUT/16-after-recovery.txt"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUT/17-events.txt"

python3 - "$OUT" "$HTTP_CODE" "$REPEAT_CODE" "$state" <<'PY'
import json, os, sys
root, first_code, repeat_code, state = sys.argv[1:]
def load(name):
    with open(os.path.join(root, name), encoding="utf-8") as stream:
        return json.load(stream)
first = load("02-buy-during-outage-response.json")
live = load("03-liveness-during-outage.json")
ready = load("04-readiness-during-outage.json")
with open(os.path.join(root, "13-order-count-after-repeat.txt"), encoding="utf-8") as stream:
    order_count = int(stream.read().strip().splitlines()[-1])
summary = {
    "dependency": "secondhand-service -> order-service",
    "fault": "order-service replicas scaled from 1 to 0",
    "faultResponseHttpStatus": int(first_code),
    "faultResponseRequestStatus": ((first.get("data") or {}).get("requestStatus")),
    "controlledResponseObserved": int(first_code) == 202 and ((first.get("data") or {}).get("requestStatus")) in {"PENDING", "RETRY"},
    "livenessDuringFault": live.get("status"),
    "readinessDuringFault": ready.get("status"),
    "recoveredRequestStatus": state,
    "repeatHttpStatus": int(repeat_code),
    "matchingOrdersAfterRepeat": order_count,
    "noDuplicateOrder": order_count == 1,
}
summary["courseFaultHandlingRequirementPassed"] = (
    summary["controlledResponseObserved"]
    and summary["livenessDuringFault"] == "UP"
    and summary["readinessDuringFault"] == "UP"
)
summary["automaticRecoveryPassed"] = (
    summary["recoveredRequestStatus"] == "CREATED" and summary["noDuplicateOrder"]
)
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump(summary, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
print(json.dumps(summary, ensure_ascii=False))
PY

echo "Dependency fault evidence: $OUT"
