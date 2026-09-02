#!/usr/bin/env bash

# Human-readable live dashboard for run_dependency_fault_experiment.sh.
# Usage:
#   watch -n 1 -c bash scripts/experiments/cloud-native/show_dependency_fault_dashboard.sh
#   watch -n 1 -c bash scripts/experiments/cloud-native/show_dependency_fault_dashboard.sh /path/to/evidence

NS="${NAMESPACE:-segroup8-cloud-exp-defense-ready}"
HOST_ROOT="${HOST_ROOT:-/root/segroup8-experiments/defense-ready}"
EVIDENCE_DIR="${1:-}"

RED='\033[1;31m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
CYAN='\033[1;36m'
BOLD='\033[1m'
RESET='\033[0m'

if [[ -z "$EVIDENCE_DIR" ]]; then
  EVIDENCE_DIR="$(
    find "$HOST_ROOT/evidence" -maxdepth 1 -type d \
      -name 'dependency-fault-*' -printf '%T@|%p\n' 2>/dev/null |
      sort -nr | head -1 | cut -d'|' -f2-
  )"
fi

friendly_name() {
  case "$1" in
    identity-governance-service) printf '%s' '身份治理' ;;
    catalog-shop-service) printf '%s' '商品目录' ;;
    order-service) printf '%s' '订单（故障目标）' ;;
    secondhand-service) printf '%s' '二手交易' ;;
    messaging-service) printf '%s' '消息通知' ;;
    benefits-finance-service) printf '%s' '福利财务' ;;
    *) printf '%s' "$1" ;;
  esac
}

json_value() {
  local file="$1" key="$2"
  python3 - "$file" "$key" <<'PY' 2>/dev/null
import json, sys
with open(sys.argv[1], encoding="utf-8") as stream:
    value = json.load(stream).get(sys.argv[2])
if isinstance(value, bool):
    print(str(value).lower())
elif value is not None:
    print(value)
PY
}

printf '\n'
printf '================================================================================\n'
printf '                       SEGROUP8 业务依赖故障实验\n'
printf '================================================================================\n'
printf '实验链路：二手交易微服务  ->  订单微服务（同步建单）\n'
printf '旁路链路：商品目录微服务  ->  订单微服务（异步 Outbox）\n'
printf '实验目标：Order故障不级联，恢复后自动补偿且不重复处理\n\n'

# Both tables intentionally read the same isolated namespace. External
# namespaces are outside this experiment and are not mixed into the dashboard.
isolation_services=(
  identity-governance-service
  catalog-shop-service
  order-service
  secondhand-service
  messaging-service
  benefits-finance-service
)

printf "${BOLD}当前隔离环境的六个微服务${RESET}  （命名空间：%s）\n" "$NS"
printf '%-31s %-18s %-10s %s\n' 'Deployment' '业务职责' 'Ready' '状态'
printf '%s\n' '--------------------------------------------------------------------------------'

isolation_rows="$(kubectl -n "$NS" get deployment "${isolation_services[@]}" \
  -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.spec.replicas}{"|"}{.status.readyReplicas}{"\n"}{end}' \
  2>/dev/null)"
declare -A isolation_desired isolation_ready
while IFS='|' read -r name desired ready; do
  [[ -z "$name" ]] && continue
  isolation_desired["$name"]="${desired:-0}"
  isolation_ready["$name"]="${ready:-0}"
done <<< "$isolation_rows"

for name in "${isolation_services[@]}"; do
  role="$(friendly_name "$name")"
  if [[ -z "${isolation_desired[$name]+present}" ]]; then
    printf '%-31s %-18s %-10s %b\n' "$name" "$role" '--' "${RED}未部署${RESET}"
    continue
  fi
  desired="${isolation_desired[$name]:-0}"
  ready="${isolation_ready[$name]:-0}"
  if [[ "$name" == order-service && "$desired" == 0 ]]; then
    state="${RED}故障已注入${RESET}"
  elif [[ "$desired" != 0 && "$ready" == "$desired" ]]; then
    state="${GREEN}正常${RESET}"
  elif [[ "$desired" == 0 ]]; then
    state="${YELLOW}已停止${RESET}"
  elif [[ "$ready" == 0 ]]; then
    state="${RED}未就绪${RESET}"
  else
    state="${YELLOW}启动或恢复中${RESET}"
  fi
  printf '%-31s %-18s %s/%-7s %b\n' "$name" "$role" "$ready" "$desired" "$state"
done

printf '\n'
printf "${BOLD}隔离环境中的故障链路${RESET}\n"
printf '%-31s %-25s %-9s %s\n' 'Deployment' '实验角色' 'Ready' '状态'
printf '%s\n' '--------------------------------------------------------------------------------'

deploy_rows="$(kubectl -n "$NS" get deployment secondhand-service order-service \
  -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.spec.replicas}{"|"}{.status.readyReplicas}{"\n"}{end}' \
  2>/dev/null)"
order_desired='?'
order_ready='?'
if [[ -z "$deploy_rows" ]]; then
  printf "${RED}无法读取命名空间 %s 中的Deployment${RESET}\n" "$NS"
else
  while IFS='|' read -r name desired ready; do
    [[ -z "$name" ]] && continue
    desired="${desired:-0}"
    ready="${ready:-0}"
    role="$(friendly_name "$name")"
    if [[ "$name" == order-service ]]; then
      order_desired="$desired"
      order_ready="$ready"
    fi
    if [[ "$name" == order-service && "$desired" == 0 ]]; then
      state="${RED}故障已注入：服务停止${RESET}"
    elif [[ "$desired" != 0 && "$ready" == "$desired" ]]; then
      state="${GREEN}正常运行${RESET}"
    elif [[ "$ready" == 0 ]]; then
      state="${RED}未就绪${RESET}"
    else
      state="${YELLOW}启动或恢复中${RESET}"
    fi
    printf '%-31s %-25s %s/%-7s %b\n' "$name" "$role" "$ready" "$desired" "$state"
  done <<< "$deploy_rows"
fi

printf '\n'
if [[ -z "$EVIDENCE_DIR" || ! -d "$EVIDENCE_DIR" ]]; then
  printf "${CYAN}实验阶段：等待启动故障实验${RESET}\n"
  printf '尚未发现 dependency-fault-* 证据目录。\n'
  exit 0
fi

printf '本轮证据：%s\n\n' "$(basename "$EVIDENCE_DIR")"

live_step='0'
live_total='7'
live_status='waiting'
live_title='等待实验启动'
live_action=''
live_expected=''
live_actual=''
live_elapsed='0'
if [[ -f "$EVIDENCE_DIR/experiment-state.json" ]]; then
  live_step="$(json_value "$EVIDENCE_DIR/experiment-state.json" step)"
  live_total="$(json_value "$EVIDENCE_DIR/experiment-state.json" totalSteps)"
  live_status="$(json_value "$EVIDENCE_DIR/experiment-state.json" status)"
  live_title="$(json_value "$EVIDENCE_DIR/experiment-state.json" title)"
  live_action="$(json_value "$EVIDENCE_DIR/experiment-state.json" action)"
  live_expected="$(json_value "$EVIDENCE_DIR/experiment-state.json" expected)"
  live_actual="$(json_value "$EVIDENCE_DIR/experiment-state.json" actual)"
  live_elapsed="$(json_value "$EVIDENCE_DIR/experiment-state.json" elapsedSeconds)"
elif [[ -f "$EVIDENCE_DIR/summary.json" ]]; then
  live_step='7'
  live_status='passed'
  live_title='历史实验已经完成'
fi

printf "${BOLD}当前测试步骤${RESET}\n"
if [[ "$live_status" == failed ]]; then
  printf "${RED}步骤 %s/%s：%s（失败）${RESET}\n" "$live_step" "$live_total" "$live_title"
elif [[ "$live_status" == passed && "$live_step" == "$live_total" ]]; then
  printf "${GREEN}步骤 %s/%s：%s${RESET}\n" "$live_step" "$live_total" "$live_title"
else
  printf "${YELLOW}步骤 %s/%s：%s${RESET}\n" "$live_step" "$live_total" "$live_title"
fi
[[ -n "$live_action" ]] && printf '正在执行：%s\n' "$live_action"
[[ -n "$live_expected" ]] && printf '预期结果：%s\n' "$live_expected"
[[ -n "$live_actual" ]] && printf '当前观测：%s\n' "$live_actual"
printf '本轮用时：%ss\n\n' "$live_elapsed"

fault_http=''
fault_request=''
[[ -f "$EVIDENCE_DIR/02-buy-during-outage-status.txt" ]] && \
  fault_http="$(tr -d '\r\n' < "$EVIDENCE_DIR/02-buy-during-outage-status.txt")"
if [[ -f "$EVIDENCE_DIR/02-buy-during-outage-response.json" ]]; then
  fault_request="$(python3 - "$EVIDENCE_DIR/02-buy-during-outage-response.json" <<'PY' 2>/dev/null
import json, sys
with open(sys.argv[1], encoding="utf-8") as stream:
    print(((json.load(stream).get("data") or {}).get("requestStatus")) or "")
PY
)"
fi

live=''
ready_health=''
[[ -f "$EVIDENCE_DIR/03-liveness-during-outage.json" ]] && \
  live="$(json_value "$EVIDENCE_DIR/03-liveness-during-outage.json" status)"
[[ -f "$EVIDENCE_DIR/04-readiness-during-outage.json" ]] && \
  ready_health="$(json_value "$EVIDENCE_DIR/04-readiness-during-outage.json" status)"

recovered=''
catalog_recovered=''
catalog_inbox=''
if [[ -f "$EVIDENCE_DIR/09-recovery-timeline.txt" ]]; then
  recovered="$(sed -n 's/.*secondhand=\([^ ]*\).*/\1/p' "$EVIDENCE_DIR/09-recovery-timeline.txt" | tail -1)"
  catalog_recovered="$(sed -n 's/.*catalog=\([^ ]*\).*/\1/p' "$EVIDENCE_DIR/09-recovery-timeline.txt" | tail -1)"
  catalog_inbox="$(sed -n 's/.*orderInbox=\([^ ]*\).*/\1/p' "$EVIDENCE_DIR/09-recovery-timeline.txt" | tail -1)"
fi

order_count=''
[[ -f "$EVIDENCE_DIR/13-order-count-after-repeat.txt" ]] && \
  order_count="$(tr -d '[:space:]' < "$EVIDENCE_DIR/13-order-count-after-repeat.txt")"

continuity_result='等待验证'
if [[ -f "$EVIDENCE_DIR/service-continuity/results.tsv" ]]; then
  continuity_ok="$(awk -F '\t' 'NR > 1 && $3 == "200" { ok++ } END { print ok+0 }' \
    "$EVIDENCE_DIR/service-continuity/results.tsv")"
  continuity_total="$(awk 'END { print (NR > 0 ? NR - 1 : 0) }' \
    "$EVIDENCE_DIR/service-continuity/results.tsv")"
  continuity_result="$continuity_ok/$continuity_total 项HTTP检查通过"
fi

printf "${BOLD}关键状态变化${RESET}\n"
if [[ "$order_desired" == 0 ]]; then
  printf '订单服务：%b\n' "${GREEN}[正常 1]${RESET} -> ${RED}[故障 0]${RESET} -> [等待恢复]"
elif [[ -f "$EVIDENCE_DIR/08-recovery.txt" ]]; then
  printf '订单服务：%b\n' "${GREEN}[正常 1]${RESET} -> ${RED}[故障 0]${RESET} -> ${GREEN}[恢复 1]${RESET}"
else
  printf '订单服务：%b\n' "${GREEN}[正常 1]${RESET} -> [等待注入] -> [等待恢复]"
fi
printf '二手请求：%s -> %s -> %s\n' '[未提交]' "[${fault_request:-等待降级}]" "[${recovered:-等待恢复}]"
printf '目录事件：PENDING -> %s，Order Inbox=%s\n\n' "${catalog_recovered:-等待恢复}" "${catalog_inbox:---}"

if [[ -f "$EVIDENCE_DIR/summary.json" ]]; then
  course_pass="$(json_value "$EVIDENCE_DIR/summary.json" courseFaultHandlingRequirementPassed)"
  recovery_pass="$(json_value "$EVIDENCE_DIR/summary.json" automaticRecoveryPassed)"
  if [[ "$course_pass" == true && "$recovery_pass" == true ]]; then
    printf "${GREEN}${BOLD}实验结论：通过（六服务故障隔离和两条依赖恢复均成功）${RESET}\n\n"
  else
    printf "${RED}${BOLD}实验结论：未通过，请查看原始证据${RESET}\n\n"
  fi
elif [[ "$order_desired" == 0 ]]; then
  printf "${RED}${BOLD}实验阶段：Order已停止，正在验证五个非Order服务${RESET}\n\n"
elif [[ "$live_step" == 5 ]]; then
  printf "${YELLOW}${BOLD}实验阶段：Order正在启动或等待自动补偿${RESET}\n\n"
else
  printf "${CYAN}${BOLD}实验阶段：故障处理与恢复验证进行中${RESET}\n\n"
fi

printf "${BOLD}关键结果${RESET}\n"
printf '故障期间购买请求：HTTP %s，业务状态=%s\n' "${fault_http:---}" "${fault_request:---}"
printf '五个非Order服务：%s\n' "$continuity_result"
printf '二手交易微服务：存活=%s，就绪=%s\n' "${live:---}" "${ready_health:---}"
printf '依赖恢复：Secondhand=%s，Catalog=%s，OrderInbox=%s\n' \
  "${recovered:-等待恢复}" "${catalog_recovered:-等待恢复}" "${catalog_inbox:---}"
printf '重复提交结果：匹配订单数=%s\n' "${order_count:-等待验证}"
printf '当前订单服务：Ready %s/%s\n' "$order_ready" "$order_desired"
printf '================================================================================\n'
