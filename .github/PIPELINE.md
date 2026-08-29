# 自动化测试与交付流水线

流水线定义位于 `.github/workflows/ci-cd.yml`。A-E 五域后端/API 测试通过
`_domain-tests.yml` 复用同一结构；浏览器层在启动 Compose 前先执行
UC01-UC25 覆盖清单门禁，不为每个 UC 分别创建 workflow。

CI 链接：
`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## 触发阶段

- Pull Request：执行前端生产构建、后端平台测试、A-E 五域测试、Helm 校验、
  UC01-UC25 覆盖清单、真实 Compose 冒烟和全量 Playwright，并上传证据。
- `main`：重复执行质量检查；全部门禁通过后，将已验证的前后端制品封装为
  Docker 镜像、推送至阿里云 ACR，并通过 Helm 部署到单节点 K3s。
- `v*` 标签：执行质量检查并创建带前后端发布包的 GitHub Release。
- 手动运行：在 Actions 页面按需执行构建和测试，不触发生产部署。

## GitHub 配置

建议至少将 `1 / Frontend common tests`、`1 / Backend platform tests and JAR`、
五个 Domain job、`2 / UC01-UC25 browser coverage manifest`、
`3 / Playwright full-stack smoke` 和 `4 / All UC E2E tests` 设置为 `main`
分支必需检查。

覆盖门禁执行：

```bash
node scripts/ci/verify-uc-e2e-coverage.mjs
```

它要求 UC01-UC25 分别在约定的 `frontend/e2e/domain-*/` 下至少有一个
`ucXX-*.spec.ts`，并拒绝放错 Domain 或超出 UC01-UC25 的文件。平台 smoke/health
spec 不计作业务 UC。报告同时写入 Actions Step Summary，并作为
`uc01-uc25-e2e-coverage` artifact 上传。当前 UC01-UC25 均已有规范位置的 spec，门禁结果为 25/25；未来任一用例入口缺失或放错 Domain 时，该门禁会按设计失败并阻断流水线。

创建 `production` Environment 并配置：

- Repository/Environment variables：`ACR_REGISTRY`、`ACR_NAMESPACE`
- Environment secrets：`ACR_USERNAME`、`ACR_PASSWORD`、`DEPLOY_HOST`、
  `DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`
- 可选 variables：`K8S_NAMESPACE`（默认 `segroup8`）、`PRODUCTION_URL`

集群中需预先创建 `acr-pull-secret`、`segroup8-backend-secret` 和
`segroup8-mysql-secret`。流水线使用不可变的 `sha-<Git SHA>` 标签发布镜像；
Helm 通过 `--atomic --wait` 等待健康探针，升级失败时自动回滚。
具体初始化和持久化说明见 `deploy/helm/segroup8/README.md`。
