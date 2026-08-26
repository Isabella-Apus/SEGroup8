# Docker Secret 配置

1. 将 `.env.secrets.example` 复制到仓库外的受控目录并替换所有 `replace_*` 值。
2. 启动容器时使用 `docker run --env-file /secure/path/segroup8.env ...` 注入。
3. 生产环境优先使用 Docker Compose `secrets` 或外部 Secret Manager；不要把真实 `.env` 复制进镜像。
4. 镜像发布时注入 `APP_VERSION`、`APP_COMMIT`、`APP_BUILD_TIME`，并用 `/actuator/info` 核验。

仓库内文件是字段模板，不是可直接用于生产的凭据文件。

