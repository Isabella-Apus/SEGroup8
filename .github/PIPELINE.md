# GitHub CI/CD 流水线

流水线定义位于 `workflows/uc06-uc10-microservices.yml`，覆盖从 Pull Request 验证到生产部署和版本发布的完整过程。

## 阶段

1. **验证与测试**：前端执行锁定依赖安装和生产构建；后端执行 Maven 测试；四个微服务执行单元、API、端到端测试，并验证 Kustomize 清单。
2. **制品**：上传 `frontend-dist`、`backend-jar` 和 Surefire 测试报告，默认保留 7 天。
3. **容器发布**：`main` 或 `v*` 标签通过测试后，将四个微服务镜像推送到 GitHub Container Registry。每个镜像同时拥有不可变的 `sha-<commit>` 标签；主分支另有 `latest`，版本标签另有同名标签。
4. **部署**：主分支可部署主应用到 systemd + Nginx 服务器，也可部署微服务到 Kubernetes。两个部署入口默认关闭。
5. **版本发布**：推送 `v*` 标签后创建 GitHub Release，附带前后端发布包。

## GitHub 配置

建议在仓库 Settings 中将 `Backend tests and package`、`Frontend build` 和 `Microservices tests` 设为 `main` 分支的必需检查，并禁止未通过检查的 Pull Request 合并。

### 主应用生产部署

创建名为 `production` 的 GitHub Environment，建议配置 required reviewers。然后配置：

- Repository variable `ENABLE_PRODUCTION_DEPLOY=true`
- Environment secrets `DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS`
- 可选 repository variables：`DEPLOY_PATH`（默认 `/srv/SE/SEGroup8`）、`BACKEND_SERVICE`（默认 `segroup8-backend`）、`NGINX_SERVICE`（默认 `nginx`）

`DEPLOY_KNOWN_HOSTS` 应保存由管理员核验过的服务器 SSH host key，不能使用未经核验的在线扫描结果。服务器上的部署用户必须能够写入 `DEPLOY_PATH`，并可无交互执行 `sudo systemctl restart <backend>`、`sudo nginx -t` 和 `sudo systemctl reload <nginx>`。部署保留 `platform-backend.previous.jar` 和 `frontend/dist.previous`，服务重启失败时自动恢复上一版。

### Kubernetes 部署

创建名为 `kubernetes-production` 的 GitHub Environment，然后配置：

- Repository variable `ENABLE_K8S_DEPLOY=true`
- Environment secret `KUBE_CONFIG`：base64 编码后的 kubeconfig
- 集群命名空间 `se-group8` 中预先存在 `commerce-db-secret`
- 若 GHCR 包不是公开包，集群还需配置有拉取权限的 `imagePullSecret`

## 发布版本

使用语义化版本标签触发发布：

```bash
git tag v1.0.0
git push origin v1.0.0
```

部署开关未启用时，相应 job 会显示为 skipped，不影响测试、制品、镜像和 Release 阶段。
