# 本地与 K3s 交付运行

本服务同时交付本地 Compose 和完整系统 K3s/Helm 自动部署。课程 HPA 与 Order 依赖故障采用系统级正式实验，不在每个微服务重复执行。

## 环境

- JDK 17、Maven 3.9+
- Docker Desktop / Docker Compose v2
- MySQL 8.4（Compose 自动提供）

## 构建与测试

```powershell
mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify
```

## 启动

先在当前 PowerShell 会话设置本地 Secret；不要提交 `.env`：

```powershell
$env:IDENTITY_MYSQL_ROOT_PASSWORD='<local-root-secret>'
$env:IDENTITY_DB_PASSWORD='<local-app-secret>'
$env:JWT_SECRET='<at-least-32-byte-secret>'
$env:INTERNAL_SERVICE_TOKEN='<local-service-token>'
$env:BOOTSTRAP_ADMIN_PASSWORD='<local-admin-password>'
docker compose -f microservices/identity-governance-service/compose.local.yml up --build -d
```

检查：

```powershell
Invoke-RestMethod http://localhost:8091/actuator/health/liveness
Invoke-RestMethod http://localhost:8091/actuator/health/readiness
Invoke-RestMethod http://localhost:8091/actuator/info
```

停止时使用 `docker compose ... down`；除非确认不再需要本地数据，不要加 `-v`。

## K3s 自动部署

身份服务使用独立的 `Identity Governance Service CI/CD`。PR 阶段执行 Maven、真实 MySQL，并从唯一已测试 JAR 构建候选镜像；流水线保存 JAR SHA-256、候选 Image ID 和 release metadata，Domain A 独立 E2E 加载并验证该镜像。合并到 `main` 后不再重新构建，而是把同一候选镜像原样 tag/push 为 ACR 的 `identity-governance:sha-<full-sha>`，保存 registry digest，再随共享 `segroup8` Helm release 原子升级。集群一次性前置条件见 `deploy/helm/segroup8/README.md`。

## Domain A 浏览器回归入口

`e2e` profile 使用既有 `frontend/dist`，并把原 Nginx 的 `backend` 别名解析到本服务，不修改根网关：

```powershell
docker compose -f microservices/identity-governance-service/compose.local.yml --profile e2e up --build -d
$env:E2E_BASE_URL='http://127.0.0.1:8089'
$env:E2E_OUTPUT_DIR='../04_tests/microservices/identity-governance-service/evidence/domain-a-playwright'
Set-Location frontend
npx playwright test e2e/domain-a --workers=1
```
