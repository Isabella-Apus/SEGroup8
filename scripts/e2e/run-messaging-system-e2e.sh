#!/usr/bin/env bash
set -Eeuo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose=(docker compose -f compose.yml -f compose.e2e.yml -f compose.messaging-system-e2e.yml)
evidence_root="${E2E_OUTPUT_DIR:-${RUNNER_TEMP:-/tmp}/messaging-system-e2e}"
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
: "${E2E_USERNAME:=user}"
: "${E2E_PASSWORD:=user123}"
: "${E2E_BASE_URL:=http://127.0.0.1:8088}"
export E2E_USERNAME E2E_PASSWORD E2E_BASE_URL

"${compose[@]}" config --quiet
"${compose[@]}" build backend frontend
"${compose[@]}" up -d --wait database backend messaging-db messaging frontend
"${compose[@]}" run --rm messaging-seed

pushd frontend >/dev/null
npm ci
npx playwright install --with-deps chromium
E2E_OUTPUT_DIR="${evidence_root}" \
  npx playwright test e2e/domain-e/uc24-chat.spec.ts e2e/domain-e/uc25-notification.spec.ts \
  --workers=1 | tee "${evidence_root}/playwright.log"
popd >/dev/null
