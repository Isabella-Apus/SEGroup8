# 自动化测试与交付流水线

流水线定义位于 `.github/workflows/ci-cd.yml`，只负责现有前端和后端项目，不引入额外业务实现或架构改造。

## 触发阶段

- Pull Request：执行前端生产构建、后端全部自动化测试和 JAR 打包，并上传测试报告与构建制品。
- `main`：重复执行质量检查；显式开启生产部署后，将已验证的前后端制品部署到 systemd + Nginx 服务器。
- `v*` 标签：执行质量检查并创建带前后端发布包的 GitHub Release。
- 手动运行：在 Actions 页面按需执行构建和测试，不触发生产部署。

## GitHub 配置

建议将 `Frontend build` 和 `Backend automated tests` 设置为 `main` 分支的必需检查。

生产部署默认关闭。启用时创建 `production` Environment 并配置：

- Repository variable：`ENABLE_PRODUCTION_DEPLOY=true`
- Environment secrets：`DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`
- 可选 variables：`DEPLOY_PATH`、`BACKEND_SERVICE`、`NGINX_SERVICE`

部署脚本保留上一版前端和后端；服务启动或 Nginx 重载失败时尝试自动恢复。
