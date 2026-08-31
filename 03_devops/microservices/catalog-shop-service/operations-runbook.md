# Operations runbook

## 发布

1. 确认 CI 的 Maven、分层测试、MySQL Compose smoke 和 Helm lint 全部通过。
2. 创建 Secret `segroup8-catalog-shop-secret`，键为 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`INTERNAL_SERVICE_TOKEN`；可选注入 `RISK_AUDIT_LLM_API_KEY`，但默认不需要。
3. 使用不可变 `sha-<git-sha>` 镜像标签执行 `.github/scripts/deploy-catalog-shop-k3s.sh`；脚本统一执行原子 Helm upgrade、失败诊断、rollout 和版本/健康/API smoke。
4. 检查 rollout、readiness、`/actuator/info` 中 commit，并执行分类、搜索、内部快照 smoke。

## 诊断

依次查看 `helm status segroup8 -n segroup8`、`kubectl describe pod -l app.kubernetes.io/component=catalog-shop -n segroup8`、`kubectl logs -l app.kubernetes.io/component=catalog-shop -n segroup8 --previous`。数据库启动错误优先查 Flyway 版本、授权和 URL；库存错误按 requestId/reservationId 查询，禁止手工直接改商品库存。

## 恢复

应用故障用 `helm rollback segroup8 <revision> -n segroup8 --wait`。数据库迁移只允许向前修复 migration；生产禁用 Flyway clean。先隔离写流量、备份 Schema，再执行修复版本。

日志不得包含 Authorization、JWT、数据库密码、内部 token、用户搜索原文以外的隐私字段。
