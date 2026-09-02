# 自动化测试与交付流水线

完整系统流水线位于 `.github/workflows/ci-cd.yml`，六个微服务各有独立流水线。共享测试模板 `_domain-tests.yml` 复用 A-E 领域测试结构；浏览器层在启动 Compose 后执行 UC01-UC25 覆盖检查和真实 Playwright 测试。

Actions 入口：<https://github.com/Isabella-Apus/SEGroup8/actions>

## 触发方式

- Pull Request：执行受影响范围的构建、单元/集成/API/E2E 与 Helm 静态验证，不部署生产环境。
- 推送 `main`：各服务流水线在测试通过后发布同一候选镜像，并在部署开关及生产凭据可用时原子部署到 K3s；完整系统流水线验证前端、兼容后端、五域和 UC01-UC25。
- 手动运行：执行构建与测试；生产部署仍受 workflow 中的 `main` 分支和部署配置约束。
- 项目不创建 GitHub Release；制品、测试报告和部署日志由 Actions artifact 保存。

## 必要配置

`production` Environment 需要：

- Variables：`ACR_REGISTRY`、`ACR_NAMESPACE`、`K8S_NAMESPACE`；
- Secrets：`ACR_USERNAME`、`ACR_PASSWORD`、`DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`；
- 集群中预先创建数据库、服务身份和镜像拉取所需 Secret。

生产镜像使用 `sha-<Git SHA>` 标签。Helm 使用 `--atomic --wait`，探针或 rollout 失败时自动回滚。完整初始化和排查方式见 `deploy/helm/segroup8/README.md` 与 `03_devops/microservices/*/operations-runbook.md`。

UC 覆盖检查：

```bash
node scripts/ci/verify-uc-e2e-coverage.mjs
```

它只检查 UC01-UC25 是否各有对应浏览器测试入口；真正的验收结论仍以 Maven、真实 MySQL、Compose/Playwright 和部署执行结果为准，文档存在性与文件 hash 不作为普通测试门禁。
