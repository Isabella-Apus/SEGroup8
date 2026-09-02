#!/usr/bin/env bash
set -euo pipefail

STATE_FILE="${1:?usage: run_dependency_fault_experiment.sh /path/to/state.env}"
source "$STATE_FILE"
OUT_NAME="${2:-dependency-fault-$(date +%Y%m%d-%H%M%S)}"
if [[ ! "$OUT_NAME" =~ ^[a-zA-Z0-9._-]+$ ]]; then
  echo "Unsafe evidence directory name: $OUT_NAME" >&2
  exit 2
fi
OUT="$HOST_ROOT/evidence/$OUT_NAME"
mkdir -p "$OUT"
{
  echo "run_id=$RUN_ID"
  echo "namespace=$NAMESPACE"
  echo "git_commit=$GIT_COMMIT"
  echo "secondhand_jar_sha256=$SECONDHAND_JAR_SHA256"
  echo "order_jar_sha256=$ORDER_JAR_SHA256"
  echo "identity_jar_sha256=$IDENTITY_JAR_SHA256"
  echo "started_at=$(date --iso-8601=seconds)"
} > "$OUT/00-run-metadata.env"
MYSQL_POD="$(kubectl -n "$NAMESPACE" get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"
MYSQL_ROOT_PASSWORD="$(kubectl -n "$NAMESPACE" get secret experiment-secrets -o jsonpath='{.data.MYSQL_ROOT_PASSWORD}' | base64 -d)"
JWT_SECRET="$(kubectl -n "$NAMESPACE" get secret experiment-secrets -o jsonpath='{.data.JWT_SECRET}' | base64 -d)"
INTERNAL_SERVICE_TOKEN="$(kubectl -n "$NAMESPACE" get secret experiment-secrets -o jsonpath='{.data.INTERNAL_SERVICE_TOKEN}' | base64 -d)"
SECONDHAND_IP="$(kubectl -n "$NAMESPACE" get service secondhand-service -o jsonpath='{.spec.clusterIP}')"
IDENTITY_IP="$(kubectl -n "$NAMESPACE" get service identity-governance-service -o jsonpath='{.spec.clusterIP}')"
CATALOG_IP="$(kubectl -n "$NAMESPACE" get service segroup8-catalog-shop -o jsonpath='{.spec.clusterIP}')"
FINANCE_IP="$(kubectl -n "$NAMESPACE" get service benefits-finance -o jsonpath='{.spec.clusterIP}')"
MESSAGING_IP="$(kubectl -n "$NAMESPACE" get service messaging -o jsonpath='{.spec.clusterIP}')"
BASE_URL="http://$SECONDHAND_IP:8080"
PRODUCTION_NAMESPACE="${PRODUCTION_NAMESPACE:-segroup8}"

curl_service() {
  curl --connect-timeout 3 --max-time 10 --retry 3 --retry-all-errors --retry-delay 2 "$@"
}

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

kubectl -n "$NAMESPACE" scale deployment secondhand-service --replicas=1 >/dev/null
kubectl -n "$NAMESPACE" rollout status deployment/secondhand-service --timeout=180s >/dev/null
kubectl -n "$NAMESPACE" get deployment,pod -o wide > "$OUT/00-before-fault.txt"
IDENTITY_CODE="$(curl_service -sS -o "$OUT/00-identity-address-snapshot.json" -w '%{http_code}' \
  "http://$IDENTITY_IP:8091/internal/users/3/address-snapshot?addressId=1" \
  -H "X-Internal-Service-Token: $INTERNAL_SERVICE_TOKEN" \
  -H 'X-Request-Id: dependency-fault-precheck')"
echo "$IDENTITY_CODE" > "$OUT/00-identity-address-snapshot-status.txt"

FAULT_START="$(date --iso-8601=seconds)"
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=0 > "$OUT/01-fault-injection.txt"
kubectl -n "$NAMESPACE" wait --for=delete pod -l app=order-service --timeout=120s >> "$OUT/01-fault-injection.txt" 2>&1 || true
kubectl -n "$NAMESPACE" get deployment,pod -o wide >> "$OUT/01-fault-injection.txt"

# Catalog depends on Order only for asynchronous inventory outcome delivery.
# Inject one deterministic event after Order is gone and observe that Catalog
# retains it for retry without making its own APIs unavailable.
CATALOG_EVENT_ID="catalog-order-fault-$(date +%s)-$$"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -uroot -p"$MYSQL_ROOT_PASSWORD" catalog_shop_db -e \
  "INSERT INTO outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload,status,attempts,destination,next_attempt_at) VALUES('$CATALOG_EVENT_ID','RESERVATION','fault-reservation','InventoryReservationReleased.v1','{\"reservationId\":900500,\"orderId\":\"fault-reservation\",\"status\":\"RELEASED\"}','PENDING',0,'ORDER',CURRENT_TIMESTAMP);"
catalog_outbox_status=""
catalog_outbox_attempts="0"
deadline=$((SECONDS + 40))
while (( SECONDS < deadline )); do
  read -r catalog_outbox_status catalog_outbox_attempts < <(
    kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" catalog_shop_db -e \
      "SELECT status,attempts FROM outbox_event WHERE event_id='$CATALOG_EVENT_ID';"
  )
  echo "$(date --iso-8601=seconds) $catalog_outbox_status $catalog_outbox_attempts" \
    >> "$OUT/01b-catalog-outbox-during-fault-timeline.txt"
  [[ "$catalog_outbox_status" == PENDING && "$catalog_outbox_attempts" -ge 1 ]] && break
  sleep 2
done
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" catalog_shop_db -e \
  "SELECT event_id,event_type,destination,status,attempts,last_error,next_attempt_at FROM outbox_event WHERE event_id='$CATALOG_EVENT_ID';" \
  > "$OUT/01b-catalog-outbox-during-fault.tsv"

# Exercise every non-Order service while the isolated Order has zero endpoints.
# Health probes alone are not enough: one representative business endpoint is
# called for Identity, Catalog, Finance, Messaging, and Secondhand.
CONTINUITY="$OUT/service-continuity"
mkdir -p "$CONTINUITY"
printf 'service\tcheck\thttp_status\tbody_file\n' > "$CONTINUITY/results.tsv"
record_request() {
  local service="$1" check="$2" body_name="$3"
  shift 3
  local body="$CONTINUITY/$body_name" code
  : > "$body"
  code="$(curl_service -sS -o "$body" -w '%{http_code}' "$@" || true)"
  printf '%s\t%s\t%s\t%s\n' "$service" "$check" "${code:-000}" "$body_name" >> "$CONTINUITY/results.tsv"
}
while read -r service service_ip port; do
  record_request "$service" liveness "$service-liveness.json" \
    "http://$service_ip:$port/actuator/health/liveness"
  record_request "$service" readiness "$service-readiness.json" \
    "http://$service_ip:$port/actuator/health/readiness"
done <<SERVICES
identity-governance $IDENTITY_IP 8091
catalog-shop $CATALOG_IP 8080
benefits-finance $FINANCE_IP 8085
messaging $MESSAGING_IP 8084
secondhand $SECONDHAND_IP 8080
SERVICES
record_request identity-governance business identity-address-snapshot.json \
  -H "X-Internal-Service-Token: $INTERNAL_SERVICE_TOKEN" \
  -H 'X-Request-Id: dependency-fault-identity' \
  "http://$IDENTITY_IP:8091/internal/users/3/address-snapshot?addressId=1"
record_request catalog-shop business catalog-category-tree.json \
  "http://$CATALOG_IP:8080/api/category/tree"
record_request benefits-finance business finance-dashboard.json \
  -H "Authorization: Bearer $TOKEN" \
  "http://$FINANCE_IP:8085/api/finance/dashboard"
record_request messaging business messaging-notifications.json \
  -H "Authorization: Bearer $TOKEN" \
  "http://$MESSAGING_IP:8084/api/notifications"
record_request secondhand business secondhand-list.json \
  "$BASE_URL/api/secondhand/list?pageNum=1&pageSize=5&keyword=Experiment"
kubectl -n "$NAMESPACE" get endpoints order-service -o yaml > "$CONTINUITY/order-endpoints-during-fault.yaml"
kubectl -n "$NAMESPACE" get deployment,pod,service -o wide > "$CONTINUITY/kubernetes-state.txt"
for deployment in identity-governance-service catalog-shop-service benefits-finance-service messaging-service secondhand-service; do
  kubectl -n "$NAMESPACE" logs "deployment/$deployment" --since-time="$FAULT_START" --timestamps \
    > "$CONTINUITY/$deployment.log" 2>&1 || true
done

# Observe namespace isolation while the experiment Order deployment is down.
# These observations are evidence, not an extra pass/fail gate: a developer
# cluster may legitimately have no production namespace.
if kubectl get namespace "$PRODUCTION_NAMESPACE" >/dev/null 2>&1; then
  {
    echo "observed_at=$(date --iso-8601=seconds)"
    echo "fault_namespace=$NAMESPACE"
    echo "production_namespace=$PRODUCTION_NAMESPACE"
    echo "production_order_endpoints=$(kubectl -n "$PRODUCTION_NAMESPACE" get endpoints segroup8-order -o jsonpath='{.subsets[*].addresses[*].ip}' 2>/dev/null || true)"
    kubectl -n "$PRODUCTION_NAMESPACE" get deployment,pod -o wide
    while read -r label service port path; do
      service_ip="$(kubectl -n "$PRODUCTION_NAMESPACE" get service "$service" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || true)"
      if [[ -z "$service_ip" ]]; then
        echo "$label=NOT_DEPLOYED"
        continue
      fi
      status="$(curl_service -sS -o "$OUT/01a-$label-health.json" -w '%{http_code}' "http://$service_ip:$port$path" || true)"
      echo "$label=$status"
    done <<'SERVICES'
backend backend 8080 /actuator/health/readiness
catalog_shop segroup8-catalog-shop 8080 /actuator/health/readiness
identity identity-governance-service 8091 /actuator/health/readiness
order segroup8-order 8085 /actuator/health/readiness
secondhand secondhand-service 8080 /actuator/health/readiness
messaging messaging 8084 /actuator/health/readiness
finance benefits-finance 8085 /actuator/health/readiness
frontend frontend 80 /health
SERVICES
  } > "$OUT/01a-production-namespace-isolation.txt" 2>&1
else
  echo "production namespace $PRODUCTION_NAMESPACE is not present; production impact observation NOT_RUN" \
    > "$OUT/01a-production-namespace-isolation.txt"
fi

HTTP_CODE="$(curl_service -sS -o "$OUT/02-buy-during-outage-response.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/secondhand/900500/buy" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data '{"addressId":1,"remark":"dependency-fault-experiment"}')"
echo "$HTTP_CODE" > "$OUT/02-buy-during-outage-status.txt"
curl_service -sS "$BASE_URL/actuator/health/liveness" > "$OUT/03-liveness-during-outage.json"
curl_service -sS "$BASE_URL/actuator/health/readiness" > "$OUT/04-readiness-during-outage.json"
curl_service -sS "$BASE_URL/api/secondhand/list?pageNum=1&pageSize=5&keyword=Experiment" > "$OUT/05-unrelated-list-during-outage.json"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e \
  "SELECT id,trade_type,trade_id,order_business_key,product_id,request_status,attempts,last_error,next_retry_at FROM trade_order_request WHERE product_id=900500;" \
  > "$OUT/06-request-state-during-outage.tsv"
kubectl -n "$NAMESPACE" logs deployment/secondhand-service --since-time="$FAULT_START" --timestamps > "$OUT/07-secondhand-outage.log" 2>&1

RECOVERY_START="$(date --iso-8601=seconds)"
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=1 > "$OUT/08-recovery.txt"
kubectl -n "$NAMESPACE" rollout status deployment/order-service --timeout=180s >> "$OUT/08-recovery.txt"
deadline=$((SECONDS + 180))
state=""
catalog_delivery_status=""
catalog_inbox_count="0"
while (( SECONDS < deadline )); do
  state="$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e "SELECT request_status FROM trade_order_request WHERE product_id=900500 ORDER BY id DESC LIMIT 1;")"
  catalog_delivery_status="$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" catalog_shop_db -e "SELECT status FROM outbox_event WHERE event_id='$CATALOG_EVENT_ID';")"
  catalog_inbox_count="$(kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e "SELECT COUNT(*) FROM inbox_event WHERE event_id='$CATALOG_EVENT_ID';")"
  echo "$(date --iso-8601=seconds) secondhand=$state catalog=$catalog_delivery_status orderInbox=$catalog_inbox_count" \
    >> "$OUT/09-recovery-timeline.txt"
  [[ "$state" == CREATED && "$catalog_delivery_status" == SENT && "$catalog_inbox_count" == 1 ]] && break
  sleep 2
done

kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e \
  "SELECT id,order_business_key,request_status,attempts,last_error,order_id,order_no,order_status FROM trade_order_request WHERE product_id=900500; SELECT id,status,version FROM secondhand_product WHERE id=900500;" \
  > "$OUT/10-recovered-secondhand-state.tsv"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT id,order_no,business_key,order_status,receiver_name,receiver_phone,receiver_province,receiver_city,receiver_detail_address FROM order_info WHERE business_key LIKE '%900500%'; SELECT COUNT(*) AS matching_order_count FROM order_info WHERE business_key LIKE '%900500%';" \
  > "$OUT/11-recovered-order-state.tsv"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT COUNT(*) FROM order_info WHERE business_key='SECONDHAND:DIRECT_BUY:900500-v1' AND receiver_name='Experiment Buyer' AND receiver_phone='13800008000' AND receiver_province='Zhejiang' AND receiver_city='Hangzhou' AND receiver_detail_address='West Lake Road 1';" \
  > "$OUT/11-address-snapshot-count.txt"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" catalog_shop_db -e \
  "SELECT event_id,event_type,destination,status,attempts,last_error,sent_at FROM outbox_event WHERE event_id='$CATALOG_EVENT_ID';" \
  > "$OUT/11b-catalog-outbox-after-recovery.tsv"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT event_id,event_type,producer,received_at FROM inbox_event WHERE event_id='$CATALOG_EVENT_ID'; SELECT COUNT(*) FROM inbox_event WHERE event_id='$CATALOG_EVENT_ID';" \
  > "$OUT/11c-order-catalog-inbox-after-recovery.tsv"

REPEAT_CODE="$(curl_service -sS -o "$OUT/12-repeat-buy-response.json" -w '%{http_code}' \
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

echo "Dependency fault evidence: $OUT"
python3 - "$OUT" "$HTTP_CODE" "$REPEAT_CODE" "$state" "$IDENTITY_CODE" \
  "$catalog_outbox_status" "$catalog_outbox_attempts" "$catalog_delivery_status" "$catalog_inbox_count" <<'PY'
import json, os, sys
root, first_code, repeat_code, state, identity_code, catalog_fault_status, catalog_fault_attempts, catalog_recovery_status, catalog_inbox_count = sys.argv[1:]
def load(name):
    with open(os.path.join(root, name), encoding="utf-8") as stream:
        return json.load(stream)
first = load("02-buy-during-outage-response.json")
live = load("03-liveness-during-outage.json")
ready = load("04-readiness-during-outage.json")
with open(os.path.join(root, "13-order-count-after-repeat.txt"), encoding="utf-8") as stream:
    order_count = int(stream.read().strip().splitlines()[-1])
with open(os.path.join(root, "11-address-snapshot-count.txt"), encoding="utf-8") as stream:
    address_snapshot_count = int(stream.read().strip().splitlines()[-1])

continuity = {}
continuity_root = os.path.join(root, "service-continuity")
with open(os.path.join(continuity_root, "results.tsv"), encoding="utf-8") as stream:
    next(stream)
    for line in stream:
        service, check, status, body_name = line.rstrip("\n").split("\t")
        try:
            with open(os.path.join(continuity_root, body_name), encoding="utf-8") as body_stream:
                body = json.load(body_stream)
        except (OSError, json.JSONDecodeError):
            body = None
        continuity.setdefault(service, {})[check] = {"httpStatus": int(status), "body": body}

def api_success(service, body):
    if not isinstance(body, dict):
        return False
    if service == "benefits-finance":
        return body.get("currency") == "CNY" and "personalBalance" in body
    return body.get("code") == 0

service_continuity = {}
for service, checks in continuity.items():
    live_check = checks.get("liveness", {})
    ready_check = checks.get("readiness", {})
    business_check = checks.get("business", {})
    service_continuity[service] = {
        "livenessHttpStatus": live_check.get("httpStatus"),
        "liveness": (live_check.get("body") or {}).get("status"),
        "readinessHttpStatus": ready_check.get("httpStatus"),
        "readiness": (ready_check.get("body") or {}).get("status"),
        "businessHttpStatus": business_check.get("httpStatus"),
        "businessResponseValid": api_success(service, business_check.get("body")),
    }
    service_continuity[service]["passed"] = (
        service_continuity[service]["livenessHttpStatus"] == 200
        and service_continuity[service]["liveness"] == "UP"
        and service_continuity[service]["readinessHttpStatus"] == 200
        and service_continuity[service]["readiness"] == "UP"
        and service_continuity[service]["businessHttpStatus"] == 200
        and service_continuity[service]["businessResponseValid"]
    )

summary = {
    "dependency": "Order outage across all six microservices; direct Secondhand and asynchronous Catalog dependencies",
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
    "identityAddressPrecheckHttpStatus": int(identity_code),
    "addressSnapshotStored": address_snapshot_count == 1,
    "nonOrderServicesDuringFault": service_continuity,
    "allNonOrderServicesOperationalDuringFault": len(service_continuity) == 5 and all(
        value["passed"] for value in service_continuity.values()
    ),
    "catalogOutboxStatusDuringFault": catalog_fault_status,
    "catalogOutboxAttemptsDuringFault": int(catalog_fault_attempts or 0),
    "catalogOutboxRetainedDuringFault": catalog_fault_status == "PENDING" and int(catalog_fault_attempts or 0) >= 1,
    "catalogOutboxStatusAfterRecovery": catalog_recovery_status,
    "catalogEventInboxCountAfterRecovery": int(catalog_inbox_count or 0),
    "catalogAutomaticRecoveryPassed": catalog_recovery_status == "SENT" and int(catalog_inbox_count or 0) == 1,
}
summary["courseFaultHandlingRequirementPassed"] = (
    summary["controlledResponseObserved"]
    and summary["livenessDuringFault"] == "UP"
    and summary["readinessDuringFault"] == "UP"
    and summary["allNonOrderServicesOperationalDuringFault"]
    and summary["catalogOutboxRetainedDuringFault"]
)
summary["automaticRecoveryPassed"] = (
    summary["recoveredRequestStatus"] == "CREATED"
    and summary["noDuplicateOrder"]
    and summary["addressSnapshotStored"]
    and summary["catalogAutomaticRecoveryPassed"]
)
with open(os.path.join(root, "summary.json"), "w", encoding="utf-8") as stream:
    json.dump(summary, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
print(json.dumps(summary, ensure_ascii=False))
raise SystemExit(0 if summary["automaticRecoveryPassed"] else 1)
PY
