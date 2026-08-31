#!/usr/bin/env bash
set -Eeuo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
evidence_root="${E2E_EVIDENCE_ROOT:-${repository_root}/04_tests/microservices/benefits-finance-service/evidence/compose-e2e}"
logs_root="${evidence_root}/logs"
container_name="segroup8-benefits-finance-e2e"
finance_image="${FINANCE_IMAGE:-segroup8/benefits-finance:e2e}"
mkdir -p "${logs_root}"

: "${JWT_SECRET:=SEGROUP8_E2E_JWT_SIGNING_KEY_2026_V1}"
: "${E2E_INTERNAL_SERVICE_TOKEN:=SEGROUP8_E2E_INTERNAL_SERVICE_TOKEN_2026}"
: "${E2E_FINANCE_DB_PASSWORD:=benefits_finance_e2e_password_2026}"
: "${E2E_FINANCE_MIGRATOR_PASSWORD:=benefits_finance_migrator_e2e_2026}"
: "${E2E_BASE_URL:=http://127.0.0.1:8088}"
: "${E2E_FINANCE_BASE_URL:=http://127.0.0.1:8085}"
: "${E2E_USERNAME:=user}"
: "${E2E_PASSWORD:=user123}"
: "${E2E_BUYER_USERNAME:=${E2E_USERNAME}}"
: "${E2E_BUYER_PASSWORD:=${E2E_PASSWORD}}"
: "${E2E_OFFICIAL_SELLER_USERNAME:=seller}"
: "${E2E_OFFICIAL_SELLER_PASSWORD:=seller123}"
export JWT_SECRET E2E_INTERNAL_SERVICE_TOKEN E2E_BASE_URL E2E_FINANCE_BASE_URL
export E2E_BUYER_USERNAME E2E_BUYER_PASSWORD
export E2E_USERNAME E2E_PASSWORD E2E_OFFICIAL_SELLER_USERNAME E2E_OFFICIAL_SELLER_PASSWORD
export E2E_OUTPUT_DIR="${evidence_root}"

write_sanitized_image_inspect() {
  docker image inspect --format \
    '{"id":{{json .Id}},"repoTags":{{json .RepoTags}},"repoDigests":{{json .RepoDigests}},"created":{{json .Created}},"architecture":{{json .Architecture}},"os":{{json .Os}},"size":{{json .Size}},"user":{{json .Config.User}},"healthcheck":{{json .Config.Healthcheck}},"labels":{"title":{{json (index .Config.Labels "org.opencontainers.image.title")}},"source":{{json (index .Config.Labels "org.opencontainers.image.source")}},"revision":{{json (index .Config.Labels "org.opencontainers.image.revision")}},"created":{{json (index .Config.Labels "org.opencontainers.image.created")}},"jarSha256":{{json (index .Config.Labels "com.segroup8.jar.sha256")}}}}' \
    "${finance_image}" >"${logs_root}/candidate-image-inspect.json"
}

write_sanitized_container_inspect() {
  docker inspect --format \
    '{"id":{{json .Id}},"name":{{json .Name}},"image":{{json .Config.Image}},"state":{"status":{{json .State.Status}},"running":{{json .State.Running}},"exitCode":{{json .State.ExitCode}},"startedAt":{{json .State.StartedAt}},"finishedAt":{{json .State.FinishedAt}},"health":{{json .State.Health}}}}' \
    "${container_name}" >"${logs_root}/benefits-finance-inspect.json" 2>/dev/null || true
}

cleanup() {
  local exit_code=$?
  docker logs "${container_name}" >"${logs_root}/benefits-finance.log" 2>&1 || true
  write_sanitized_container_inspect
  docker compose -f "${repository_root}/compose.yml" ps --all >"${logs_root}/compose-ps.txt" 2>&1 || true
  if [[ "${KEEP_FINANCE_SERVICE:-false}" != "true" ]]; then
    docker rm -f "${container_name}" >"${logs_root}/benefits-finance-remove.log" 2>&1 || true
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

cd "${repository_root}"
git_sha="$(git rev-parse HEAD)"
app_version="${APP_VERSION:-e2e}"
app_commit="${APP_COMMIT:-${git_sha}}"
app_build_time="${APP_BUILD_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

docker compose config --quiet | tee "${logs_root}/compose-config.log"
docker compose up -d --build --wait database backend frontend | tee "${logs_root}/compose-up.log"

docker compose exec -T database sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' <<SQL
CREATE DATABASE IF NOT EXISTS benefits_finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'benefits_finance_app'@'%' IDENTIFIED BY '${E2E_FINANCE_DB_PASSWORD}';
ALTER USER 'benefits_finance_app'@'%' IDENTIFIED BY '${E2E_FINANCE_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'benefits_finance_migrator'@'%' IDENTIFIED BY '${E2E_FINANCE_MIGRATOR_PASSWORD}';
ALTER USER 'benefits_finance_migrator'@'%' IDENTIFIED BY '${E2E_FINANCE_MIGRATOR_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON benefits_finance_db.* TO 'benefits_finance_app'@'%';
GRANT ALL PRIVILEGES ON benefits_finance_db.* TO 'benefits_finance_migrator'@'%';
FLUSH PRIVILEGES;
SQL

if [[ "${SKIP_MAVEN_VERIFY:-false}" != "true" ]]; then
  mvn -B --no-transfer-progress -f microservices/pom.xml -pl benefits-finance-service -am clean verify \
    | tee "${logs_root}/maven-verify.log"
fi

jar_sha256="not-applicable-prebuilt-image"
if [[ "${SKIP_FINANCE_IMAGE_BUILD:-false}" != "true" ]]; then
  mapfile -t candidate_jars < <(find microservices/benefits-finance-service/target -maxdepth 1 -type f \
    -name 'benefits-finance-service-*.jar' ! -name '*.jar.original' -print)
  if [[ "${#candidate_jars[@]}" -ne 1 ]]; then
    echo "expected exactly one verified candidate JAR, found ${#candidate_jars[@]}" >&2
    exit 1
  fi
  jar_sha256="$(sha256sum "${candidate_jars[0]}" | awk '{print $1}')"
  docker build --pull=false -t "${finance_image}" \
    --build-arg VCS_REF="${app_commit}" \
    --build-arg BUILD_TIME="${app_build_time}" \
    --build-arg JAR_SHA256="${jar_sha256}" \
    microservices/benefits-finance-service \
    | tee "${logs_root}/docker-build.log"
fi
write_sanitized_image_inspect
printf 'gitSha=%s\nappVersion=%s\nappCommit=%s\nappBuildTime=%s\njarSha256=%s\nfinanceImage=%s\n' \
  "${git_sha}" "${app_version}" "${app_commit}" "${app_build_time}" "${jar_sha256}" "${finance_image}" \
  | tee "${logs_root}/candidate-metadata.txt"

docker rm -f "${container_name}" >/dev/null 2>&1 || true
docker run -d --name "${container_name}" --network segroup8-platform \
  -p 127.0.0.1:8085:8085 \
  -e DB_URL='jdbc:mysql://database:3306/benefits_finance_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=benefits_finance_app \
  -e DB_PASSWORD="${E2E_FINANCE_DB_PASSWORD}" \
  -e FLYWAY_DB_USERNAME=benefits_finance_migrator \
  -e FLYWAY_DB_PASSWORD="${E2E_FINANCE_MIGRATOR_PASSWORD}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e INTERNAL_SERVICE_TOKEN="${E2E_INTERNAL_SERVICE_TOKEN}" \
  -e APP_VERSION="${app_version}" \
  -e APP_COMMIT="${app_commit}" \
  -e APP_BUILD_TIME="${app_build_time}" \
  "${finance_image}" | tee "${logs_root}/benefits-finance-container-id.txt"

ready=false
for _ in $(seq 1 90); do
  if curl --noproxy '*' --fail --silent "${E2E_FINANCE_BASE_URL}/actuator/health/readiness" \
      | tee "${logs_root}/readiness.json" | grep -q '"status":"UP"'; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "${ready}" != "true" ]]; then
  echo "benefits-finance readiness did not become UP" >&2
  exit 1
fi
curl --noproxy '*' --fail --silent "${E2E_FINANCE_BASE_URL}/actuator/info" \
  | tee "${logs_root}/info.json" | grep -q '"name":"benefits-finance-service"'

cd "${repository_root}/frontend"
npm ci | tee "${logs_root}/npm-ci.log"
if [[ "${SKIP_BROWSER_INSTALL:-false}" != "true" ]]; then
  npx playwright install --with-deps chromium | tee "${logs_root}/playwright-install.log"
fi
npx playwright test \
  e2e/domain-e/uc21-voucher-lifecycle.spec.ts \
  e2e/domain-e/uc22-claim-checkout.spec.ts \
  e2e/domain-e/uc23-wallet-settlement.spec.ts \
  --workers=1 | tee "${logs_root}/playwright.log"
