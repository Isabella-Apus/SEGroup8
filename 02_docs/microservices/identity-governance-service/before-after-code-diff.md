# 改造前后两个代码版本及差异

## 可复现版本

| 版本 | Git 证据 | 运行入口 | UC01-UC05 同断言结果 |
|---|---|---|---|
| 改造前单体 | `monolith-start` / `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119` | 根目录 `compose.yml` | 2026-08-30 复跑，5/5 PASS，9.9 秒 |
| 改造后微服务 | `feature/ms-identity-governance` / PR HEAD | `microservices/identity-governance-service/compose.local.yml` | 2026-08-29 复跑，5/5 PASS，33.0 秒 |

两次运行都使用当前仓库 `frontend/e2e/domain-a/uc01-uc05*.spec.ts`，没有复制或改写断言。单体基线使用临时 worktree、独立端口、独立网络和全新 MySQL 卷；发现旧 Compose 写死全局卷名后先停止实例，只在临时 worktree 改名再重跑，`monolith-start` tag 本身没有移动或覆盖。

改造前启动与回归命令的等价形式：

```powershell
git worktree add --detach C:/Users/isabe/Desktop/SE/tmp/identity-monolith-start monolith-start
$env:MYSQL_HOST_PORT='13317'
$env:BACKEND_HOST_PORT='18089'
$env:FRONTEND_HOST_PORT='18088'
docker compose -p identity-monolith-baseline up -d --build
$env:E2E_BASE_URL='http://127.0.0.1:18088'
npx playwright test e2e/domain-a
```

## 结构差异

| 关注点 | 单体版本 | 微服务版本 |
|---|---|---|
| 代码边界 | `backend` 内的 User/Admin/Report/Credit Controller、Service、Mapper | 唯一 Boot 模块 `microservices/identity-governance-service` |
| 数据 | 共享 `segroup8_platform` 与单体 `schema.sql` | `identity_governance_db`、独立账号、Flyway `V1`，禁止跨库 |
| 身份 | 单体内部 JWT 与用户上下文 | 签发 JWT；其他服务复用 `security-contract` 本地验签 |
| 跨服务一致性 | 单体本地直接调用/共享表 | 版本化事件、事务 outbox、内部最小 API、幂等键 |
| 可观测性 | 单体健康检查 | JSON 日志、liveness、readiness、info 版本与 request/trace ID |
| 交付 | 单体 Docker/Helm | 独立 JAR、镜像、Deployment、Service、可关闭 HPA、独立回滚 |

PR 的代码差异应以 `origin/main` 的起点 `bb72290cff96c78ab189468b82db1f8ba3cd9323` 到 PR HEAD 为准；不要用 `monolith-start...HEAD` 的全仓库差异冒充本 PR 改动，因为该 tag 之后还包含其他团队工作。

`microservices-v1` 只能在全组服务完成、同数据性能对比和最终验收后统一创建，本 PR 不提前打该 tag。
