# 订单服务改造前后代码差异

## 对比基线

- 改造前：`origin/main@bb72290cff96c78ab189468b82db1f8ba3cd9323`，订单能力位于单体 `backend`。
- 改造后：当前 `feature/ms-order` HEAD；验收时以 PR #210 的 GitHub HEAD SHA 为准。
- 可复现命令：

```bash
git diff --stat bb72290cff96c78ab189468b82db1f8ba3cd9323...HEAD -- \
  microservices/order-service frontend deploy/helm/segroup8 .github/workflows/order-service-ci-cd.yml
```

## 主要变化

| 项目 | 改造前 | 改造后 |
|---|---|---|
| 构建与运行 | 与单体后端共同构建、启动 | 独立 Maven 模块、Boot JAR、Dockerfile 和候选镜像 |
| 数据归属 | 订单表由单体直接访问 | `order_db` 及 Flyway V1 独占订单、售后、物流、评价、幂等、Saga 与 outbox 表 |
| 跨域协作 | 进程内调用和共享数据库 | 经 catalog、finance、secondhand 契约调用，使用超时、幂等键、状态查询和补偿 |
| 入口路由 | 前端全部进入通用 `/api` 后端 | 订单具体路径优先代理至 `order-service`，未切流路径保留单体 fallback |
| 测试 | 单体 API/浏览器回归 | 单元、全部公开 API、契约、真实 MySQL、候选镜像独立 E2E 与 UC11-15/20 Compose E2E |
| 交付 | 单体镜像和统一发布 | 独立命名流水线、JAR SHA、候选 Image ID、原样发布、Helm 原子升级和诊断 artifact |

完整接口、表和调用差异分别以 `openapi.yaml`、`database-ownership.md` 和
`cross-service-calls.md` 为准，不用文件数量代替行为验收。
