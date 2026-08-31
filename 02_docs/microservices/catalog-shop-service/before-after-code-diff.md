# catalog-shop-service 改造前后代码差异

## 对比基线

- 改造前：`origin/main@bb72290cff96c78ab189468b82db1f8ba3cd9323`，catalog、shop、risk、behavior 是四个原型模块，生产业务仍主要依赖单体。
- 改造后：当前 `codex/ms-catalog-shop` HEAD；验收以 PR #216 的 GitHub HEAD SHA 为准。
- 可复现命令：

```bash
git diff --stat bb72290cff96c78ab189468b82db1f8ba3cd9323...HEAD -- \
  microservices/catalog-shop-service frontend deploy/helm/segroup8 .github/workflows/ci-cd-microservices.yml
```

## 主要变化

| 项目 | 改造前 | 改造后 |
|---|---|---|
| 服务结构 | 四个实验性原型与单体实现并存 | 一个可独立部署的 `catalog-shop-service`，内部保留 catalog/shop/risk/behavior 分层 |
| 构建 | 原型分别存在但不形成目标服务交付 | 独立 Maven/Boot JAR、Dockerfile、Compose 和候选镜像；父 POM 暂保留旧模块以支持联合回归 |
| 数据归属 | 商品、店铺、类目和风控事实分散 | `catalog_shop_db` 与 Flyway 独占其业务表，其他服务只经 API/事件获取 |
| 跨服务 | 共享代码或单体调用 | identity、order、secondhand、messaging 使用登记 DNS、超时、幂等和 outbox/EventEnvelope |
| 测试 | 旧 Domain B 与原型测试 | 全部公开 API、真实 MySQL、权限、运行时 OpenAPI、候选镜像独立 E2E 和共享完整系统 Domain B |
| 发布 | 无独立不可变交付 | 保存 JAR SHA、候选 Image ID、当前 SHA 验收摘要，E2E 后原样推送并记录 registry digest |

旧四模块只作为合并期兼容基线保留；完成六服务切流和联合回归后再通过独立 PR 删除，避免本分支覆盖其他服务的父 POM。
