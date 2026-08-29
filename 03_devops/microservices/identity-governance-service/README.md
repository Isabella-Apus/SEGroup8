# 本地交付与运行

本服务本次仅交付本地容器化，不包含 Kubernetes/Helm/HPA。

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
