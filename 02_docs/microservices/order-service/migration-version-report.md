# 迁移版本报告

- 基线：`monolith-start`（须由仓库管理员确认/补建 tag）。
- 分支基点：`origin/main@bb72290c`，2026-08-29 拉取后创建 `feature/ms-order`。
- 迁移脚本：`V1__order_schema.sql`，新建 order_db 独占表、幂等、Saga 和 outbox。
- 目标镜像：`${ACR_REGISTRY}/${ACR_NAMESPACE}/order:sha-<git-sha>`。
- 目标集合 tag：`microservices-v1`，仅在 PR 合并、镜像 digest 和部署证据齐备后由管理员创建。

本报告不虚构尚未产生的 merge SHA、镜像 digest 或 Helm revision；这些值由 CI/部署报告回填。
