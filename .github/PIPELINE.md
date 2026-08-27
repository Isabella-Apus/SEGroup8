# 自动化测试与交付流水线

流水线定义位于 `.github/workflows/ci-cd.yml`，其中包含一个统一的 Domain A
定向入口，不为 UC01-UC05 分别创建 workflow。

CI 链接：
`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## 触发阶段

- Pull Request：执行 Domain A 定向测试、前端生产构建、后端全部自动化测试和
  JAR 打包、真实 Compose + Playwright E2E，并上传 Domain A、后端和浏览器证据。
- `main`：重复执行质量检查；显式开启生产部署后，将已验证的前后端制品部署到 systemd + Nginx 服务器。
- `v*` 标签：执行质量检查并创建带前后端发布包的 GitHub Release。
- 手动运行：在 Actions 页面按需执行构建和测试，不触发生产部署。

## GitHub 配置

建议将 `Frontend build`、`Backend automated tests` 和 `Real full-stack Playwright E2E`
设置为 `main` 分支的必需检查。Domain A 定向测试在 `Backend automated tests`
作业中先执行，结果以 `domain-a-surefire-reports` artifact 保存。

生产部署默认关闭。启用时创建 `production` Environment 并配置：

- Repository variable：`ENABLE_PRODUCTION_DEPLOY=true`
- Environment secrets：`DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`
- 可选 variables：`DEPLOY_PATH`、`BACKEND_SERVICE`、`NGINX_SERVICE`

部署脚本保留上一版前端和后端；服务启动或 Nginx 重载失败时尝试自动恢复。
