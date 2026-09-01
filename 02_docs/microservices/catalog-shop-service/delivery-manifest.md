# 交付清单

| 类别 | 位置 | 状态 |
|---|---|---|
| 可执行服务、迁移、分层测试、Dockerfile、Compose | `microservices/catalog-shop-service/` | 已交付并通过 Maven verify |
| 架构与契约 | `02_docs/microservices/catalog-shop-service/` | 已交付 |
| 前后版本差异与共同清单 | `before-after-code-diff.md`、`../service-acceptance-checklist.md` | 已交付 |
| Helm、HPA、探针 | `deploy/helm/segroup8/` | 已交付模板 |
| 运维说明 | `03_devops/microservices/catalog-shop-service/` | 已交付 |
| E2E 源码 | `frontend/e2e/domain-b/` | 复用现有 UC06-UC10 |
| 测试报告骨架和本地报告 | `04_tests/microservices/catalog-shop-service/` | 已交付；集群证据待目标环境运行 |
| 管理记录 | `05_management/microservices/catalog-shop-service/` | 模板已交付；人员/PR 待真实填写 |
| CI/CD 与部署诊断 | `.github/workflows/ci-cd-microservices.yml`、`.github/scripts/deploy-catalog-shop-k3s.sh` | 候选镜像一次构建，保存 JAR SHA/Image ID/当前 SHA 摘要，E2E 后原样发布并记录 digest；原子部署与失败日志采集 |
