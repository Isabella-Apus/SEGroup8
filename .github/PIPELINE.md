# 自动化测试与交付流水线

流水线定义位于 `.github/workflows/ci-cd.yml`，其中包含一个统一的 Domain A
  定向入口，不为 UC01-UC05 分别创建 workflow；共享 JWT security-contract
  也在同一条 PR 流程中执行。

CI 链接：
`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## 触发阶段

- Pull Request：执行 Domain A 定向测试、前端生产构建、后端全部自动化测试和
  JAR 打包、真实 Compose + Playwright E2E，并上传 Domain A、后端和浏览器证据。
- `main`：重复执行质量检查；显式开启生产部署后，将已验证的前后端制品封装为
  Docker 镜像、推送至阿里云 ACR，并通过 Helm 部署到单节点 K3s。
- `v*` 标签：执行质量检查并创建带前后端发布包的 GitHub Release。
- 手动运行：在 Actions 页面按需执行构建和测试，不触发生产部署。

## GitHub 配置

建议将 `Frontend build`、`Backend automated tests` 和 `Real full-stack Playwright E2E`
设置为 `main` 分支的必需检查。Domain A 定向测试在 `Backend automated tests`
  作业中先执行，结果以 `domain-a-surefire-reports` artifact 保存；全局 JWT
  契约结果以 `platform-security-contract-surefire-reports` artifact 保存。

生产部署默认关闭。启用时创建 `production` Environment 并配置：

- Repository/Environment variables：`ENABLE_PRODUCTION_DEPLOY=true`、
  `ACR_REGISTRY`、`ACR_NAMESPACE`
- Environment secrets：`ACR_USERNAME`、`ACR_PASSWORD`、`DEPLOY_HOST`、
  `DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`
- 可选 variables：`K8S_NAMESPACE`（默认 `segroup8`）、`PRODUCTION_URL`

集群中需预先创建 `acr-pull-secret`、`segroup8-backend-secret` 和
`segroup8-mysql-secret`。流水线使用不可变的 `sha-<Git SHA>` 标签发布镜像；
Helm 通过 `--atomic --wait` 等待健康探针，升级失败时自动回滚。
具体初始化和持久化说明见 `deploy/helm/segroup8/README.md`。
