#!/usr/bin/env bash
set -Eeuo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose=(docker compose -f compose.messaging-service-e2e.yml)
evidence_root="${E2E_OUTPUT_DIR:-${RUNNER_TEMP:-/tmp}/messaging-service-e2e}"
mkdir -p "${evidence_root}"

cleanup() {
  local status=$?
  "${compose[@]}" logs --no-color --timestamps >"${evidence_root}/compose.log" 2>&1 || true
  "${compose[@]}" down --remove-orphans >"${evidence_root}/compose-down.log" 2>&1 || true
  exit "${status}"
}
trap cleanup EXIT

cd "${repository_root}"
: "${MESSAGING_IMAGE:?MESSAGING_IMAGE must point to the tested candidate image}"

"${compose[@]}" config --quiet
"${compose[@]}" up -d --wait messaging-db identity-stub messaging
"${compose[@]}" run --rm messaging-seed

for attempt in $(seq 1 30); do
  if curl --noproxy '*' --fail --silent http://127.0.0.1:18084/actuator/health/readiness | grep -q 'UP'; then
    break
  fi
  [[ "${attempt}" -eq 30 ]] && { echo 'messaging-service did not become ready' >&2; exit 1; }
  sleep 2
done

pushd frontend >/dev/null
npm ci
npx playwright install --with-deps chromium
popd >/dev/null

AUDIT_JWT_SECRET=SEGROUP8_E2E_MESSAGING_SECRET_2026 \
AUDIT_SERVICE_TOKEN=e2e-internal-token \
AUDIT_OPERATIONS_TOKEN=e2e-operations-token \
MESSAGING_BASE_URL=http://127.0.0.1:18084 \
node scripts/ci/run-messaging-public-api-audit.mjs | tee "${evidence_root}/public-api-audit.json"

AUDIT_JWT_SECRET=SEGROUP8_E2E_MESSAGING_SECRET_2026 \
node scripts/ci/run-messaging-ws-audit.mjs | tee "${evidence_root}/websocket-audit.json"
