# Kinda Goods 购物与二手交易平台

面向校园/社区的购物与二手交易系统。前端使用 Vue 3、Vite、Pinia 和 Element Plus；后端使用 Java 17、Spring Boot 3、MySQL 8；最终系统包含兼容后端和六个已独立构建、测试、制作镜像及部署的微服务。

## 最终交付入口

- [最终目录与提交清单](FINAL_DELIVERY_STRUCTURE.md)
- [软件需求说明书](02_docs/specifications/software-requirements.md)
- [软件概要设计说明书](02_docs/specifications/software-architecture-design.md)
- [软件详细设计说明书](02_docs/specifications/software-detailed-design.md)
- [需求追溯矩阵](02_docs/specifications/requirements-traceability-matrix.md)
- [微服务划分、接口与数据归属](02_docs/architecture/README.md)
- [测试汇总](02_docs/test-summary.md)
- [云原生与性能实验最终报告](03_devops/cloud-native-experiments/README.md)
- [测试代码与原始证据索引](04_tests/README.md)

期中报告、旧分支验收稿、重复交付清单、旧单服务 HPA 和完整 Playwright HTML/trace/video 已移至仓库外 `selfwork/SEGroup8-final-prune-20260902/`，不作为最终提交事实来源。

## 系统组成

| 组件 | 目录 | 责任 |
|---|---|---|
| Vue 前端 | `frontend/` | 用户、卖家、管理端页面与 Playwright E2E |
| 兼容后端 | `backend/` | 未切流能力、兼容路径与完整系统基线 |
| identity-governance-service | `microservices/identity-governance-service/` | 登录、用户、地址、商家审核、信用与审计 |
| catalog-shop-service | `microservices/catalog-shop-service/` | 分类、商品、店铺、搜索、库存与行为 |
| order-service | `microservices/order-service/` | 订单、支付状态、履约、售后、物流与评价 |
| secondhand-service | `microservices/secondhand-service/` | 二手发布、直购、议价、拍卖与建单补偿 |
| messaging-service | `microservices/messaging-service/` | 会话、通知、WebSocket 与事件收件箱 |
| benefits-finance-service | `microservices/benefits-finance-service/` | 优惠券、钱包、支付、退款与结算 |

服务划分图、接口清单、数据表归属和跨服务调用统一维护在 `02_docs/architecture/`，各服务目录只保留边界、改造前后差异、必要契约和追溯材料。

## 本地运行

环境要求：JDK 17、Maven 3.8+、Node.js 18+、npm、MySQL 8；或者直接使用 Docker Compose。

```powershell
docker compose -f compose.yml up -d --build
```

默认入口：

- 前端：`http://127.0.0.1:8088`
- 后端健康检查：`http://127.0.0.1:8089/actuator/health`
- MySQL：`127.0.0.1:3307`

详细说明见 [DOCKER.md](DOCKER.md)。本地直接启动时，从示例生成私有配置，不要提交真实密码：

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml `
  backend/src/main/resources/application-local.yml
mvn -f backend/pom.xml spring-boot:run

Set-Location frontend
npm install
npm run dev:real
```

## 构建与测试

```powershell
mvn -B -f backend/pom.xml test
mvn -B -f microservices/pom.xml test

Set-Location frontend
npm install
npm run build:real
npx playwright test
```

完整系统和六个微服务使用独立 GitHub Actions 流水线。推送到 `main` 后，各服务只在相关路径变化时构建与测试；通过后发布 `sha-<完整提交号>` 的不可变镜像，并在生产环境开关开启时使用 Helm 原子部署。完整流程及探针、版本、日志和回滚命令见 `03_devops/`、`.github/workflows/` 与 `.github/scripts/`。

## Kubernetes 与实验

- Helm chart：`deploy/helm/segroup8/`
- K3s 部署脚本：`.github/scripts/deploy-*-k3s.sh`
- 完整系统 HPA：`scripts/experiments/cloud-native/run_system_hpa_experiment.sh`
- Order 依赖故障：`scripts/experiments/cloud-native/run_dependency_fault_experiment.sh`

正式 HPA 不是二手微服务 HPA。它以共享系统后端为可伸缩对象，配置为 CPU 60%、`minReplicas=2`、`maxReplicas=4`；正式结果为 `2 → 4（4 Ready）→ 2`。故障实验只暂停隔离命名空间中的 Order，不修改生产 Order。

## 安全说明

仓库不保存生产密码、JWT、SSH 凭据、数据库 Secret 或渲染后的含密配置。Actuator 只公开健康、就绪、指标和版本所需端点；生产不公开 Flyway 迁移详情。其余要求见 [SECURITY.md](SECURITY.md)。
