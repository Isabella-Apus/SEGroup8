#!/usr/bin/env bash
set -Eeuo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
default_evidence_root="${repository_root}/04_tests/platform-e2e/evidence"

resolve_configured_path() {
  local configured_path="${1:-}"
  local fallback_path="${2}"
  local resolved_path="${configured_path:-${fallback_path}}"

  if [[ "${resolved_path}" != /* ]]; then
    resolved_path="${repository_root}/${resolved_path}"
  fi

  mkdir -p "${resolved_path}"
  (cd "${resolved_path}" && pwd)
}

configured_evidence_root="${E2E_EVIDENCE_ROOT:-}"
configured_output_root="${E2E_OUTPUT_DIR:-}"

# 新脚手架优先使用 E2E_EVIDENCE_ROOT。
# 只有 E2E_OUTPUT_DIR 时，兼容 test/domain-a-infra 的旧行为。
if [[ -n "${configured_evidence_root}" ]]; then
  evidence_root="$(
    resolve_configured_path \
      "${configured_evidence_root}" \
      "${default_evidence_root}"
  )"
elif [[ -n "${configured_output_root}" ]]; then
  evidence_root="$(
    resolve_configured_path \
      "${configured_output_root}" \
      "${default_evidence_root}"
  )"
else
  evidence_root="$(
    resolve_configured_path \
      "" \
      "${default_evidence_root}"
  )"
fi

# 同时设置两个变量时，Playwright 输出可以独立于 Compose 日志。
# 只设置 E2E_OUTPUT_DIR 时，两者仍保持在同一个 UC 目录。
if [[ -n "${configured_evidence_root}" && -n "${configured_output_root}" ]]; then
  playwright_output_root="$(
    resolve_configured_path \
      "${configured_output_root}" \
      "${evidence_root}"
  )"
else
  playwright_output_root="${evidence_root}"
fi

export E2E_OUTPUT_DIR="${playwright_output_root}"
logs_root="${evidence_root}/logs"
mkdir -p "${logs_root}"
rm -f "${logs_root}/failure-stage.txt"

cd "${repository_root}"

keep_services="${KEEP_COMPOSE:-false}"
compose_started="false"
current_stage="initialization"

run_logged() {
  local log_name="$1"
  shift
  set +e
  "$@" 2>&1 | tee "${logs_root}/${log_name}"
  local command_status=${PIPESTATUS[0]}
  set -e
  return "${command_status}"
}

collect_diagnostics() {
  set +e
  docker compose ps --all >"${logs_root}/compose-ps.txt" 2>&1
  docker compose config >"${logs_root}/compose-config.yml" 2>&1
  docker compose logs --no-color --timestamps >"${logs_root}/compose.log" 2>&1
  for service in frontend backend database; do
    docker compose logs --no-color --timestamps "${service}" >"${logs_root}/${service}.log" 2>&1
  done
  set -e
}

wait_for_container_health() {
  local service="$1"
  local timeout_seconds="${2:-180}"
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    local container_id
    container_id="$(docker compose ps -q "${service}" 2>/dev/null || true)"
    if [[ -n "${container_id}" ]]; then
      local state
      state="$(docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${container_id}" 2>/dev/null || true)"
      echo "${service}: ${state}"
      [[ "${state}" == "running|healthy" ]] && return 0
      [[ "${state}" == exited\|* || "${state}" == dead\|* ]] && {
        echo "${service} stopped before becoming healthy (${state})" >&2
        return 1
      }
    else
      echo "${service}: container not created yet"
    fi
    sleep 2
  done
  echo "Timed out waiting for Compose service '${service}' to become healthy" >&2
  return 1
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local pattern="$3"
  local timeout_seconds="${4:-120}"
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    local body
    body="$(curl --noproxy '*' --fail --silent --show-error "${url}" 2>/dev/null || true)"
    if [[ -n "${body}" ]] && grep -Eq "${pattern}" <<<"${body}"; then
      echo "${name}: HTTP ready (${url})"
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for ${name} HTTP endpoint: ${url}" >&2
  return 1
}

on_exit() {
  local status=$?
  if [[ "${compose_started}" == "true" ]]; then
    collect_diagnostics
    if [[ "${keep_services}" != "true" ]]; then
      docker compose down --remove-orphans >"${logs_root}/compose-down.log" 2>&1 || true
    fi
  fi
  if [[ "${status}" -ne 0 ]]; then
    {
      printf 'stage=%s\n' "${current_stage}"
      printf 'error=exit %s\n' "${status}"
    } >"${logs_root}/failure-stage.txt"
  fi
  exit "${status}"
}
trap on_exit EXIT

: "${E2E_USERNAME:=user}"
: "${E2E_PASSWORD:=user123}"
: "${E2E_ROLE:=USER}"
: "${E2E_BASE_URL:=http://127.0.0.1:8088}"
export E2E_USERNAME E2E_PASSWORD E2E_ROLE E2E_BASE_URL
export E2E_OUTPUT_DIR="${playwright_output_root}"

if [[ "${RESET_DATABASE:-false}" == "true" ]]; then
  current_stage="database-reset"
  echo "RESET_DATABASE=true: removing only the Compose project and its named database volume"
  run_logged compose-reset.log docker compose down -v --remove-orphans
fi

compose_started="true"
current_stage="compose-config"
run_logged compose-config-check.log docker compose config --quiet
current_stage="compose-build"
run_logged compose-build.log docker compose build backend frontend
current_stage="database-start"
run_logged database-start.log docker compose up -d database
current_stage="database-health"
wait_for_container_health database
current_stage="backend-start"
run_logged backend-start.log docker compose up -d backend
current_stage="backend-health"
wait_for_container_health backend
current_stage="backend-http"
wait_for_http backend http://127.0.0.1:8089/actuator/health '"status"[[:space:]]*:[[:space:]]*"UP"'
current_stage="frontend-start"
run_logged frontend-start.log docker compose up -d frontend
current_stage="frontend-health"
wait_for_container_health frontend
current_stage="frontend-http"
wait_for_http frontend http://127.0.0.1:8088/health '^ok'

cd "${repository_root}/frontend"
current_stage="npm-install"
run_logged npm-ci.log npm ci
current_stage="browser-install"
run_logged playwright-install.log npx playwright install --with-deps chromium
current_stage="playwright"
set +e
npx playwright test "$@" 2>&1 | tee "${logs_root}/playwright.log"
test_exit_code=${PIPESTATUS[0]}
set -e
exit "${test_exit_code}"
