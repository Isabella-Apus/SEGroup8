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
LIVE_STATE="$OUT/experiment-state.json"
EXPERIMENT_FINISHED=false
EXPERIMENT_STARTED_AT=$SECONDS

announce() {
  local step="$1" title="$2"
  printf '\n\033[1;36m======================================================================\033[0m\n'
  printf '\033[1;36m步骤 %s/7：%s\033[0m\n' "$step" "$title"
  printf '\033[1;36m======================================================================\033[0m\n'
}

update_state() {
  local step="$1" status="$2" title="$3" action="$4" expected="$5" actual="$6"
  python3 - "$LIVE_STATE" "$OUT_NAME" "$step" "$status" "$title" "$action" \
    "$expected" "$actual" "$((SECONDS - EXPERIMENT_STARTED_AT))" <<'PY'
import datetime, json, os, sys

path, run_name, step, status, title, action, expected, actual, elapsed = sys.argv[1:]
value = {
    "schemaVersion": 1,
    "runName": run_name,
    "step": int(step),
    "totalSteps": 7,
    "status": status,
    "title": title,
    "action": action,
    "expected": expected,
    "actual": actual,
    "elapsedSeconds": int(elapsed),
    "updatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
}
temporary = path + ".tmp"
with open(temporary, "w", encoding="utf-8") as stream:
    json.dump(value, stream, ensure_ascii=False, indent=2)
    stream.write("\n")
os.replace(temporary, path)
PY
}

on_exit() {
  local status=$?
  if [[ $status -ne 0 && "$EXPERIMENT_FINISHED" != true ]]; then
    set +e
    update_state 7 failed '实验异常退出' '恢复订单微服务，避免故障残留' \
      'order-service恢复为1个Ready副本' "实验退出码=$status"
    kubectl -n "$NAMESPACE" scale deployment order-service --replicas=1 >/dev/null 2>&1
    kubectl -n "$NAMESPACE" rollout status deployment/order-service --timeout=180s >/dev/null 2>&1
    set -e
  fi
  exit "$status"
}
trap on_exit EXIT

announce 1 '检查隔离环境并展示全部六个微服务'
update_state 1 running '实验准备' '检查六个微服务并清理900500号专用业务样本' \
  '六个微服务全部Ready，专用样本可购买' '正在检查'
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

curl_service() {
  curl --connect-timeout 3 --max-time 10 --retry 3 --retry-all-errors --retry-delay 2 "$@"
}

MICROSERVICE_DEPLOYMENTS=(
  identity-governance-service
  catalog-shop-service
  order-service
  secondhand-service
  benefits-finance-service
  messaging-service
)

show_microservices() {
  local evidence_file="$1"
  {
    printf '隔离命名空间：%s\n' "$NAMESPACE"
    printf '%-32s %-8s %-8s %-10s\n' SERVICE READY DESIRED AVAILABLE
    kubectl -n "$NAMESPACE" get deployment "${MICROSERVICE_DEPLOYMENTS[@]}" \
      -o custom-columns='SERVICE:.metadata.name,READY:.status.readyReplicas,DESIRED:.spec.replicas,AVAILABLE:.status.availableReplicas' \
      --no-headers
  } | tee "$evidence_file"
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

kubectl -n "$NAMESPACE" scale deployment order-service secondhand-service --replicas=1 >/dev/null
for deployment in "${MICROSERVICE_DEPLOYMENTS[@]}"; do
  kubectl -n "$NAMESPACE" rollout status "deployment/$deployment" --timeout=180s >/dev/null
done
kubectl -n "$NAMESPACE" get deployment,pod -o wide > "$OUT/00-before-fault.txt"
show_microservices "$OUT/00-six-microservices-before-fault.txt"
IDENTITY_CODE="$(curl_service -sS -o "$OUT/00-identity-address-snapshot.json" -w '%{http_code}' \
  "http://$IDENTITY_IP:8091/internal/users/3/address-snapshot?addressId=1" \
  -H "X-Internal-Service-Token: $INTERNAL_SERVICE_TOKEN" \
  -H 'X-Request-Id: dependency-fault-precheck')"
echo "$IDENTITY_CODE" > "$OUT/00-identity-address-snapshot-status.txt"
update_state 1 passed '实验准备完成' '六个微服务和业务样本检查完成' \
  '六个微服务Ready，身份地址预检HTTP 200' "六个微服务已展示，身份地址预检HTTP $IDENTITY_CODE"

announce 2 '只停止隔离环境中的订单微服务'
update_state 2 running '故障注入' '将隔离环境中的order-service从1副本缩为0副本' \
  '六个服务仍显示在表中，Order Ready 0/0' '正在停止Order Pod'
FAULT_START="$(date --iso-8601=seconds)"
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=0 > "$OUT/01-fault-injection.txt"
kubectl -n "$NAMESPACE" wait --for=delete pod -l app=order-service --timeout=120s >> "$OUT/01-fault-injection.txt" 2>&1 || true
kubectl -n "$NAMESPACE" get deployment,pod -o wide >> "$OUT/01-fault-injection.txt"
show_microservices "$OUT/01-six-microservices-during-fault.txt"
order_ready="$(kubectl -n "$NAMESPACE" get deployment order-service -o jsonpath='{.status.readyReplicas}' 2>/dev/null)"
order_ready="${order_ready:-0}"
update_state 2 passed '订单微服务已停止' 'order-service副本1 → 0，其他五个服务保持运行' \
  'Order Ready 0/0，其他五个服务仍可见' "Order Ready $order_ready/0，六服务表已刷新"

announce 3 '故障期间执行二手商品购买'
update_state 3 running '验证受控降级' 'Order不可用时提交900500号商品购买请求' \
  'HTTP 202，业务状态RETRY或PENDING' '正在发送购买请求'
HTTP_CODE="$(curl_service -sS -o "$OUT/02-buy-during-outage-response.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/secondhand/900500/buy" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data '{"addressId":1,"remark":"dependency-fault-experiment"}')"
echo "$HTTP_CODE" > "$OUT/02-buy-during-outage-status.txt"
FAULT_REQUEST_STATUS="$(python3 - "$OUT/02-buy-during-outage-response.json" <<'PY' 2>/dev/null
import json, sys
with open(sys.argv[1], encoding="utf-8") as stream:
    print(((json.load(stream).get("data") or {}).get("requestStatus")) or "UNKNOWN")
PY
)"
if [[ "$HTTP_CODE" == '202' && ( "$FAULT_REQUEST_STATUS" == 'RETRY' || "$FAULT_REQUEST_STATUS" == 'PENDING' ) ]]; then
  update_state 3 passed '受控降级符合预期' '故障期间购买请求已可靠保存' \
    'HTTP 202 + RETRY/PENDING' "HTTP $HTTP_CODE + $FAULT_REQUEST_STATUS"
else
  update_state 3 failed '受控降级不符合预期' '故障期间购买请求结果异常' \
    'HTTP 202 + RETRY/PENDING' "HTTP $HTTP_CODE + $FAULT_REQUEST_STATUS"
fi

announce 4 '验证其他五个微服务没有被Order故障级联影响'
update_state 4 running '验证故障隔离' '检查五个非Order服务的存活、就绪和代表业务接口' \
  '五个服务的三类检查全部HTTP 200，Catalog事件被保留' '正在逐服务检查'

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
curl_service -sS "$BASE_URL/actuator/health/liveness" > "$OUT/03-liveness-during-outage.json"
curl_service -sS "$BASE_URL/actuator/health/readiness" > "$OUT/04-readiness-during-outage.json"
curl_service -sS "$BASE_URL/api/secondhand/list?pageNum=1&pageSize=5&keyword=Experiment" > "$OUT/05-unrelated-list-during-outage.json"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" secondhand_db -e \
  "SELECT id,trade_type,trade_id,order_business_key,product_id,request_status,attempts,last_error,next_retry_at FROM trade_order_request WHERE product_id=900500;" \
  > "$OUT/06-request-state-during-outage.tsv"
kubectl -n "$NAMESPACE" logs deployment/secondhand-service --since-time="$FAULT_START" --timestamps > "$OUT/07-secondhand-outage.log" 2>&1

column -t -s $'\t' "$CONTINUITY/results.tsv" 2>/dev/null || cat "$CONTINUITY/results.tsv"
if awk -F '\t' 'NR > 1 && $3 != "200" { failed=1 } END { exit failed }' "$CONTINUITY/results.tsv" \
  && [[ "$catalog_outbox_status" == PENDING && "$catalog_outbox_attempts" -ge 1 ]]; then
  update_state 4 passed '五个非Order服务均正常' '逐服务检查完成，Catalog异步事件已可靠保留' \
    '15个HTTP检查为200，Catalog Outbox=PENDING且已重试' \
    "五个服务全部可用，Catalog=$catalog_outbox_status/attempts=$catalog_outbox_attempts"
else
  update_state 4 failed '非Order服务连续性检查异常' '至少一个接口或Catalog Outbox状态不符合预期' \
    '15个HTTP检查为200，Catalog Outbox=PENDING且已重试' \
    "请检查service-continuity/results.tsv，Catalog=$catalog_outbox_status/attempts=$catalog_outbox_attempts"
fi

announce 5 '恢复订单服务并观察两条依赖自动恢复'
update_state 5 running '依赖恢复' '将Order恢复为1副本，分别计时应用启动和后台补偿' \
  'Order Ready 1/1；Secondhand变为CREATED；Catalog事件变为SENT且只消费一次' '正在启动Order'
RECOVERY_START="$(date --iso-8601=seconds)"
RECOVERY_STEP_START=$SECONDS
ORDER_ROLLOUT_START=$SECONDS
kubectl -n "$NAMESPACE" scale deployment order-service --replicas=1 > "$OUT/08-recovery.txt"
kubectl -n "$NAMESPACE" rollout status deployment/order-service --timeout=180s >> "$OUT/08-recovery.txt"
ORDER_ROLLOUT_SECONDS=$((SECONDS - ORDER_ROLLOUT_START))
printf 'Order已Ready，启动耗时：%ss；现在等待后台自动补偿。\n' "$ORDER_ROLLOUT_SECONDS"
update_state 5 running 'Order已恢复，等待业务补偿' 'Order Pod已Ready，后台任务正在重试两条依赖' \
  'Secondhand=CREATED；Catalog=SENT；Order Inbox=1' "Order启动耗时=${ORDER_ROLLOUT_SECONDS}s"
RECOVERY_POLL_START=$SECONDS
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
  printf '补偿等待：%3ss，Secondhand=%s，Catalog=%s，OrderInbox=%s\n' \
    "$((SECONDS - RECOVERY_POLL_START))" "${state:-UNKNOWN}" "${catalog_delivery_status:-UNKNOWN}" "${catalog_inbox_count:-0}"
  update_state 5 running '等待自动补偿' 'Order已Ready，后台任务正在重试两条依赖' \
    'Secondhand=CREATED；Catalog=SENT；Order Inbox=1' \
    "Order启动=${ORDER_ROLLOUT_SECONDS}s，Secondhand=${state:-UNKNOWN}，Catalog=${catalog_delivery_status:-UNKNOWN}，Inbox=${catalog_inbox_count:-0}"
  [[ "$state" == CREATED && "$catalog_delivery_status" == SENT && "$catalog_inbox_count" == 1 ]] && break
  sleep 2
done
RECOVERY_POLL_SECONDS=$((SECONDS - RECOVERY_POLL_START))
RECOVERY_STEP_SECONDS=$((SECONDS - RECOVERY_STEP_START))
if [[ "$state" == CREATED && "$catalog_delivery_status" == SENT && "$catalog_inbox_count" == 1 ]]; then
  update_state 5 passed '两条依赖均已自动恢复' 'Order启动和后台补偿均完成' \
    'Secondhand=CREATED；Catalog=SENT；Order Inbox=1' \
    "Order启动=${ORDER_ROLLOUT_SECONDS}s，补偿等待=${RECOVERY_POLL_SECONDS}s，总计=${RECOVERY_STEP_SECONDS}s"
else
  update_state 5 failed '依赖恢复超时' '等待180秒后至少一条依赖仍未恢复' \
    'Secondhand=CREATED；Catalog=SENT；Order Inbox=1' \
    "Order启动=${ORDER_ROLLOUT_SECONDS}s，补偿等待=${RECOVERY_POLL_SECONDS}s，Secondhand=${state:-UNKNOWN}，Catalog=${catalog_delivery_status:-UNKNOWN}，Inbox=${catalog_inbox_count:-0}"
fi

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

announce 6 '重复提交相同请求，验证不会重复创建订单'
update_state 6 running '验证幂等性' '再次提交900500号商品的相同购买请求' \
  '重复请求HTTP 200，最终匹配订单数为1' '正在重复提交并统计订单'
REPEAT_CODE="$(curl_service -sS -o "$OUT/12-repeat-buy-response.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/secondhand/900500/buy" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data '{"addressId":1,"remark":"dependency-fault-experiment-repeat"}')"
echo "$REPEAT_CODE" > "$OUT/12-repeat-buy-status.txt"
kubectl -n "$NAMESPACE" exec "$MYSQL_POD" -- mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" order_db -e \
  "SELECT COUNT(*) FROM order_info WHERE business_key LIKE '%900500%';" > "$OUT/13-order-count-after-repeat.txt"
ORDER_COUNT="$(tr -d '[:space:]' < "$OUT/13-order-count-after-repeat.txt")"
if [[ "$REPEAT_CODE" == '200' && "$ORDER_COUNT" == '1' ]]; then
  update_state 6 passed '幂等性验证完成' '重复请求没有创建第二张订单' \
    '重复请求HTTP 200，最终匹配订单数=1' "HTTP $REPEAT_CODE，订单数=$ORDER_COUNT"
else
  update_state 6 failed '幂等性验证未通过' '重复请求响应或订单数量异常' \
    '重复请求HTTP 200，最终匹配订单数=1' "HTTP $REPEAT_CODE，订单数=$ORDER_COUNT"
fi
kubectl -n "$NAMESPACE" logs deployment/secondhand-service --since-time="$RECOVERY_START" --timestamps > "$OUT/14-secondhand-recovery.log" 2>&1
kubectl -n "$NAMESPACE" logs deployment/order-service --since-time="$RECOVERY_START" --timestamps > "$OUT/15-order-recovery.log" 2>&1
kubectl -n "$NAMESPACE" get deployment,pod -o wide > "$OUT/16-after-recovery.txt"
kubectl -n "$NAMESPACE" get events --sort-by=.lastTimestamp > "$OUT/17-events.txt"

announce 7 '汇总全部证据并给出实验结论'
update_state 7 running '生成实验结论' '汇总六服务连续性、两条依赖恢复和幂等性证据' \
  '课程故障处理要求和自动恢复均通过' '正在生成summary.json'
echo "Dependency fault evidence: $OUT"
if python3 - "$OUT" "$HTTP_CODE" "$REPEAT_CODE" "$state" "$IDENTITY_CODE" \
  "$catalog_outbox_status" "$catalog_outbox_attempts" "$catalog_delivery_status" "$catalog_inbox_count" \
  "$ORDER_ROLLOUT_SECONDS" "$RECOVERY_POLL_SECONDS" "$RECOVERY_STEP_SECONDS" <<'PY'
import json, os, sys
root, first_code, repeat_code, state, identity_code, catalog_fault_status, catalog_fault_attempts, catalog_recovery_status, catalog_inbox_count, order_rollout_seconds, recovery_poll_seconds, recovery_step_seconds = sys.argv[1:]
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
    "orderRolloutSeconds": int(order_rollout_seconds),
    "recoveryPollingSeconds": int(recovery_poll_seconds),
    "dependencyRecoveryStepSeconds": int(recovery_step_seconds),
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
raise SystemExit(0 if (
    summary["courseFaultHandlingRequirementPassed"]
    and summary["automaticRecoveryPassed"]
) else 1)
PY
then
  update_state 7 passed '实验全部通过' '六服务连续性、故障降级、恢复和幂等验证全部完成' \
    '全部验证项通过' \
    "六服务检查通过；Order启动=${ORDER_ROLLOUT_SECONDS}s；补偿=${RECOVERY_POLL_SECONDS}s；总恢复=${RECOVERY_STEP_SECONDS}s"
  printf '\n\033[1;32m故障实验通过：六个服务均显示，五个非Order服务可用，两条依赖自动恢复。\033[0m\n'
  python3 -m json.tool "$OUT/summary.json"
  EXPERIMENT_FINISHED=true
else
  result=$?
  update_state 7 failed '实验未通过' '至少一个最终断言未满足' \
    '全部验证项通过' "请检查summary.json（exit=$result）"
  python3 -m json.tool "$OUT/summary.json" 2>/dev/null || true
  EXPERIMENT_FINISHED=true
  exit "$result"
fi
