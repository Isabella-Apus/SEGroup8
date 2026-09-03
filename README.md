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
- [最终技术交付报告（可编辑版）](02_docs/final-delivery-report.md)
- [最终技术交付报告（PDF）](02_docs/final-delivery-report.pdf)
- [云原生与性能实验最终报告](03_devops/cloud-native-experiments/README.md)
- [测试代码与原始证据索引](04_tests/README.md)



## 版本与 Git 追溯

- 代码仓库：<https://github.com/Isabella-Apus/SEGroup8>
- 改造前原系统标签：`monolith-start`，对应提交 `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119`。
- 改造后六微服务 Maven 版本：`1.0.0`；容器发布版本使用不可变的 `sha-<完整 Git SHA>`，不使用 `latest` 作为验收依据。
- 六服务生产部署、完整系统 HPA、Order 故障恢复和性能实验的共同运行证据基线：`b622e6bbb0447d6823b50e7789e4777f7131eb9b`。该 SHA 是实验基线，不表示此后文档和测试修复停止提交。
- 完整提交记录：<https://github.com/Isabella-Apus/SEGroup8/commits/main/>；本地可执行 `git log --date=iso --pretty=fuller --decorate` 查看作者、提交者、时间、标签和合并记录。

六份服务级改造前后代码差异位于 `02_docs/microservices/<service>/before-after-code-diff.md`。

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

## 环境与版本

| 类别 | 课程验收基线 |
|---|---|
| Java / Maven | JDK 17；Maven 3.8 或更高版本 |
| 后端框架 | Spring Boot 3.3.4、MyBatis-Plus 3.5.7 |
| Node.js / 前端 | Node.js 20、Vue 3.5.13、Vite 6.2.0 |
| 数据库 | MySQL 8.4.6，字符集 `utf8mb4` |
| 容器 | Docker Engine 与 Docker Compose v2 |
| 集群 | K3s v1.36.3+k3s1（正式实验环境）、Helm 3 |

Windows 本地开发使用 PowerShell；CI 使用 GitHub Actions Ubuntu runner。版本来源分别为 `backend/pom.xml`、`microservices/pom.xml`、`frontend/package.json`、`compose.yml`、工作流和正式实验环境清单。

## 端口、探针与版本地址

根 `compose.yml` 是兼容系统本地基线，只启动 frontend、兼容 backend、MySQL、catalog-shop 及其数据库；它不是六微服务生产拓扑。六微服务完整拓扑由 `deploy/helm/segroup8/` 和各服务流水线部署到 K3s。

| 组件 | 容器/K8s 端口 | 根 Compose 默认宿主机入口 | K8s Service DNS |
|---|---:|---|---|
| frontend | 80 | `http://127.0.0.1:8088` | `http://frontend:80` |
| 兼容 backend | 8080 | `http://127.0.0.1:8089` | `http://backend:8080` |
| MySQL | 3306 | `127.0.0.1:3307` | `mysql:3306` |
| identity-governance | 8091 | 服务验收编排按需映射 | `http://identity-governance-service:8091` |
| catalog-shop | 8080 | `http://127.0.0.1:8086` | `http://segroup8-catalog-shop:8080` |
| order | 8085 | 服务验收编排按需映射 | `http://segroup8-order:8085` |
| secondhand | 8080 | 服务验收编排按需映射 | `http://secondhand-service:8080` |
| messaging | 8084 | 服务验收编排按需映射 | `http://messaging:8084` |
| benefits-finance | 8085 | 服务验收编排按需映射 | `http://benefits-finance:8085` |

本地直接检查：

- 前端健康：`http://127.0.0.1:8088/health`
- 兼容后端健康：`http://127.0.0.1:8089/actuator/health`
- 兼容后端存活：`http://127.0.0.1:8089/actuator/health/liveness`
- 兼容后端就绪：`http://127.0.0.1:8089/actuator/health/readiness`
- 兼容后端版本：`http://127.0.0.1:8089/actuator/info`
- catalog-shop 就绪：`http://127.0.0.1:8086/actuator/health/readiness`

六个微服务统一提供 `/actuator/health`、`/actuator/health/liveness`、`/actuator/health/readiness` 和 `/actuator/info`；在集群内把上表 Service DNS 与这些路径拼接即可。生产公网只暴露 Ingress 批准的业务路由和必要 Actuator 端点，不公开 Flyway、`env`、`beans` 或 `heapdump`。

## 启动方法

### Docker Compose（推荐的本地验收方式）

```powershell
Copy-Item .env.docker.example .env
docker compose -f compose.yml config --quiet
docker compose -f compose.yml up -d --build
docker compose -f compose.yml ps
```

浏览器打开 `http://127.0.0.1:8088`。详细说明和取证脚本见 [DOCKER.md](DOCKER.md)。

### 源码直接启动

先启动 MySQL 8 并创建 `segroup8_platform`，再从示例生成私有配置。不要提交 `application-local.yml`、`.env` 或真实密码。

```powershell
Copy-Item backend/src/main/resources/application-local.example.yml `
  backend/src/main/resources/application-local.yml
mvn -f backend/pom.xml spring-boot:run

Set-Location frontend
npm install
npm run dev:real
```

六个微服务的独立启动、真实 MySQL 验收和 Compose 文件见各自的 `microservices/<service>/README.md` 与 `03_devops/microservices/<service>/README.md`。完整 K3s 部署前置条件、Secret 键名和 Helm 操作见 [Helm 部署说明](deploy/helm/segroup8/README.md)。

## 本地测试账号

以下账号只由 `docker/mysql/02-seed.sql` 写入全新本地 Compose 数据卷，不用于生产环境：

| 角色 | 用户名 | 密码 | 用途 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 管理端、审核与仲裁测试 |
| 官方卖家 | `seller` | `seller123` | 店铺、商品、发货与结算测试 |
| 普通用户 | `user` | `user123` | 默认 E2E 买家账号 |
| 第二普通用户 | `third` | `third123` | 聊天、议价、拍卖与多人场景 |

CI 可通过 `E2E_USERNAME`、`E2E_PASSWORD`、`E2E_ROLE` 等环境变量覆盖测试账号。请勿将个人账号或生产 Token 写入仓库。

## 初始数据

首次创建 Compose MySQL 数据卷时按文件名顺序自动执行：

1. `backend/src/main/resources/schema.sql`：建立兼容系统表结构；
2. `docker/mysql/02-seed.sql`：写入四个测试账号、演示店铺、分类、商品、地址、余额及 UC12—UC15 所需订单夹具；
3. catalog-shop 使用自己的 MySQL schema 和 Flyway migration，健康后由 `docker/catalog-shop/catalog-shop.sql` 写入独立演示数据。

MySQL 官方镜像只会在空数据目录执行初始化脚本。修改 seed 后，已有命名卷不会自动重放。确需从零初始化时可执行下面的命令，但它会永久删除本地 Compose 数据，执行前必须确认数据可以丢弃：

```powershell
docker compose -f compose.yml down -v
docker compose -f compose.yml up -d --build
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
