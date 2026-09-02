# 01_source 源码提交清单

为避免破坏 Compose、Maven、前端代理、Helm 和 CI 的相对路径，开发仓库暂不把源码物理移动到本目录。本目录是最终压缩包的源码清单。

最终提交时，把下列内容按原相对结构放入 `01_source/SEGroup8/`：

- `backend/`
- `frontend/`
- `microservices/`
- `docker/`
- `deploy/`
- `sql/`
- `scripts/`
- `.github/workflows/` 与 `.github/scripts/`
- `compose.yml`、`compose.e2e.yml`、`compose.messaging-service-e2e.yml`、`compose.messaging-system-e2e.yml`
- `.env.docker.example`、`.gitattributes`、`.gitignore`
- `README.md`、`DOCKER.md`、`DEPLOY_ALIYUN.md`、`SECURITY.md`

不要放入 `01_source`：`.git/`、`node_modules/`、`target/`、`dist/`、本地上传文件、日志、私有 `application-local.yml`、Playwright HTML/trace/video、服务器凭据或 Kubernetes Secret。

如果课程允许提交“仓库清单 + Git 地址”，可保留当前结构并只提交本文件作为源码索引；如果必须是六级目录压缩包，再在最终打包副本中移动，日常开发仓库不要改路径。
