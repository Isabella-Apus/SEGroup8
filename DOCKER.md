# Issue #65：前端、后端和数据库容器化运行

本方案使用 Docker Compose 同时构建并运行 Vue 前端、Spring Boot 后端和 MySQL 8 数据库。三个服务位于同一内部网络，前端 Nginx 将 `/api/`、`/uploads/` 和 `/ws/` 请求转发到后端。

## 一键启动

要求：Docker Desktop 已启动，Docker Engine 可用。

```powershell
docker compose -f compose.yml config --quiet
docker compose -f compose.yml up -d --build
docker compose -f compose.yml ps
```

默认访问地址：

- 前端：<http://127.0.0.1:8088>
- 前端健康检查：<http://127.0.0.1:8088/health>
- 后端健康检查：<http://127.0.0.1:8089/actuator/health>
- MySQL：`127.0.0.1:3307`

端口选择为 8088、8089 和 3307，避免和本地开发环境常用的 5174、8080、3306 冲突。需要调整时，复制 `.env.docker.example` 为 `.env` 后修改。

## 验证与取证

```powershell
powershell -ExecutionPolicy Bypass -File .\04_tests\issue-65\collect-evidence.ps1
```

脚本会校验 Compose 配置、三个容器状态、前后端健康接口、数据库连通性和初始化表数量，并把原始输出保存到 `04_tests/issue-65/evidence/`。任一关键检查失败时脚本以非零状态退出。

## 运维命令

```powershell
docker compose -f compose.yml logs --no-color
docker compose -f compose.yml restart
docker compose -f compose.yml down
```

如需同时清除数据库卷并重新导入初始化 SQL，可执行 `docker compose -f compose.yml down -v`。该命令会删除容器数据库数据，使用前必须确认数据可以丢弃。

示例密码仅用于本地验收，不应直接用于生产环境。生产环境必须通过环境变量或密钥管理系统提供数据库密码和 JWT 密钥。
