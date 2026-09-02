# 最终提交目录清单

## 课程目录

```text
SEGroup8/
├─ 01_source/                 源码打包说明（开发期间不移动源码）
├─ 02_docs/                   需求、设计、模型、测试说明、微服务改造文档
├─ 03_devops/                 CI/CD、K3s/Helm 运维及最终实验报告
├─ 04_tests/                  结构化测试结果、关键截图、最终实验原始数据
├─ 05_management/             团队后续补充管理材料
├─ 06_defense/                团队后续补充答辩材料
├─ backend/                   改造前兼容后端及测试
├─ frontend/                  Vue 前端及 Playwright
├─ microservices/             六个微服务和共享安全契约
├─ deploy/ docker/ sql/       部署及数据库初始化
├─ scripts/ .github/          测试、实验、流水线与部署脚本
└─ compose*.yml               本地和 E2E 编排
```

## 最终打包到 01_source

在提交副本中创建 `01_source/SEGroup8/`，保持以下相对路径不变：

1. `backend/`、`frontend/`、`microservices/`；
2. `docker/`、`deploy/`、`sql/`、`scripts/`；
3. `.github/workflows/`、`.github/scripts/`；
4. 所有 `compose*.yml`；
5. `.env.docker.example`、`.gitattributes`、`.gitignore`；
6. `README.md`、`DOCKER.md`、`DEPLOY_ALIYUN.md`、`SECURITY.md`、`AGENTS.md`。

不要包含 `.git/`、`node_modules/`、`target/`、`dist/`、上传文件、运行日志、私有环境配置、密码/Secret 或完整 Playwright HTML/trace/video。当前不移动源码，避免破坏 Maven、Compose、Helm 和 CI 的相对路径。

## 必交内容位置

| 内容 | 位置 |
|---|---|
| 需求/概要/详细设计 | `02_docs/specifications/` |
| UC01-UC25 模型、测试、追溯 | `02_docs/UC01/` 至 `UC25/` |
| 服务划分图、接口清单、表归属 | `02_docs/architecture/` |
| 六服务边界、调用、改造差异 | `02_docs/microservices/` |
| Docker、CI/CD、K8s/Helm | `.github/`、`deploy/`、`docker/`、`03_devops/` |
| 自动化测试证据 | 源码测试目录、`frontend/e2e/`、`04_tests/` |
| 性能、完整系统 HPA、Order 故障 | `03_devops/cloud-native-experiments/README.md`、`04_tests/cloud-native-experiments/` |
| 日志、健康、就绪、版本操作 | `03_devops/microservices/*/operations-runbook.md` |

仓库外归档：`C:\Users\isabe\Desktop\SE\selfwork\SEGroup8-final-prune-20260902\`。其中的过程稿可追溯，但不参与最终提交。
