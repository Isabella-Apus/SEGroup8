# 二手服务改造前后代码差异

## 对比基线

- 改造前：`origin/main@bb72290cff96c78ab189468b82db1f8ba3cd9323`，二手商品和交易逻辑位于单体 `backend`。
- 改造后：当前 `feature/ms-secondhand` HEAD；验收时以 PR #213 的 GitHub HEAD SHA 为准。
- 可复现命令：

```bash
git diff --stat bb72290cff96c78ab189468b82db1f8ba3cd9323...HEAD -- \
  microservices/secondhand-service frontend deploy/helm/segroup8 .github/workflows/secondhand-service-ci-cd.yml
```

## 主要变化

| 项目 | 改造前 | 改造后 |
|---|---|---|
| 构建与运行 | 单体后端共同构建 | 独立 Maven/Boot JAR、Dockerfile、候选镜像和 Compose |
| 数据归属 | 单体直接访问二手相关表 | `secondhand_db` 与 Flyway V1/V2 独占商品、议价、竞拍、成交请求、幂等和 outbox |
| 订单协作 | 进程内下单 | 经 order 内部契约携带幂等键请求，未知结果先查状态，失败由恢复任务重试 |
| 路由 | 通用 `/api` 进入单体 | 二手具体路径优先进入 `secondhand-service`，UC20 再跨服务调用订单 |
| 测试 | 单体 API/浏览器测试 | 单元、21 个公开操作、契约、真实 MySQL、独立 UC16-19 E2E 与完整 UC16-20 E2E |
| 交付 | 单体发布 | 独立流水线、JAR SHA、候选 Image ID、原样推送、Helm 探针/安全上下文/回滚诊断 |

性能、HPA 和依赖故障材料是课程实验补充，不替代接口、数据库和完整系统 E2E 门禁。
