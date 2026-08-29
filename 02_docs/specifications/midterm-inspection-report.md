# Kinda Goods 中期检查总报告

> 检查日期：2026-08-29  
> 原系统基线：`monolith-start`；当前中期基线：main `09db0eed`；整理分支：`restructure-whole`  
> 状态口径：`PASS` 表示存在相应执行证据；`PARTIAL` 表示只有部分证据或部分实现；`NOT_RUN` 表示本分支未执行；`TARGET` 表示目标设计，不能当作已部署事实。

## 1. 报告范围与推荐检查顺序

本报告是中期检查的单一文字入口。依据课程任务书“8 月 29 日中期检查要看到什么”，检查内容应覆盖：原系统及全部确认用例可运行并打 Git 标签；全部用例的需求、系统级图、组件级图、对象级图和追溯表；前端、后端、数据库容器和自动构建测试；微服务划分图、服务接口清单和数据表归属方案。

在原有设想上补入了容易漏检的四项：**Git 基线/标签、课程要求对照、证据口径与复现命令、未完成项**。建议助教按以下顺序检查：

1. 课程要求对照和 Git 基线；
2. 前端、后端、MySQL 启动与健康状态；
3. 25 个确认用例总体运行情况；
4. 逐用例 API/集成测试与真实浏览器 E2E；
5. 四份权威文档及需求、图模型、追溯位置；
6. CI/CD 构建、测试、制品、镜像和部署；
7. 微服务边界、服务接口和数据库归属；
8. 当前缺口、现场复现命令和中期结论。

## 2. 课程中期要求对照

| 课程检查项 | 当前结论 | 可核验证据 |
|---|---|---|
| 原系统能够启动并保留基线 | `PASS`（已有基线证据） | Git 标签 `monolith-start`；[源码与基线说明](../../01_source/README.md) |
| 确认后的 UC01–UC25 均有运行入口 | `PASS`（静态范围） | [用例总清单](../use-case-catalog.md)；25/25 Playwright spec 覆盖门禁 |
| 全部用例真实浏览器运行 | `PASS`（已核验 main CI）；本分支 `NOT_RUN` | [main 全 UC E2E job](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611)；本分支未重跑 Compose |
| 全部用例的需求、系统级图、组件级图、对象级图和追溯表 | `PASS`（文档） | 四份权威文档；25 个 UC 目录；156 份可编辑 Mermaid 源码 |
| 前端、后端、数据库容器能够启动 | `PASS`（已核验 main CI）；本分支 `NOT_RUN` | [Compose](../../compose.yml)；[main CI](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952) |
| CI 自动构建和测试 | `PASS` | 前端、后端、Domain A–E、覆盖门禁、Compose Playwright 均在流水线内 |
| 微服务划分图、服务接口清单、数据表归属 | `PASS`（设计交付） | [微服务边界](../architecture/microservice-boundaries.md)、[API 清单](../architecture/service-api-catalog.md)、[表归属](../architecture/database-ownership.md) |
| 至少 3 个业务微服务独立实现和部署 | `PARTIAL` | Domain B 有 4 个可独立测试的迁移原型；6 个目标业务服务尚未全部形成独立部署单元 |

结论：**中期要求中的文档、用例入口、已核验 main 流水线和目标微服务设计已形成可交付材料；但不能宣称“全部工程任务完成”**。主要缺口是 UC06–UC10 缺仓库内逐 UC Playwright JSON，以及 6 个目标业务微服务尚未全部独立实现、拆库和部署。

## 3. 原系统、分支和标签

| 项目 | 内容 |
|---|---|
| 原系统冻结标签 | `monolith-start` |
| 本轮文档所依据 main 基线 | `09db0eed` |
| 中期目录整理分支 | `restructure-whole` |
| 四份当前权威文档 | `02_docs/specifications/` |
| 上学期完整文档 | 历史基线，仅供追溯，不与当前权威文档并列作为“最新版” |

四份新文档应作为本学期针对最新 25 个用例的持续更新版。无需把它们机械拼回上学期 PDF；若助教要求一个提交包，应提交本报告、四份权威文档和可编辑 Mermaid 源文件，上学期资料仅作为历史附件。

## 4. 前端、后端和数据库启动情况

当前单体全栈由 [compose.yml](../../compose.yml) 定义，并由 [compose.e2e.yml](../../compose.e2e.yml) 为浏览器测试补充配置。

| 组件 | 镜像/实现 | 对外入口 | 健康检查与依赖 | 结论 |
|---|---|---|---|---|
| MySQL | `mysql:8.4.6` | `localhost:3307` | `mysqladmin ping`；先装载 `schema.sql`，再装载测试种子 | 已核验 main CI `PASS`；本分支 `NOT_RUN` |
| 后端 | Spring Boot，`segroup8/backend:1.0.0` | `localhost:8089` | `/actuator/health` 返回 `UP`；等待 MySQL healthy | 已核验 main CI `PASS`；本分支 Maven 233/233 `PASS` |
| 前端 | Vue 3 + Nginx，`segroup8/frontend:1.0.0` | `http://localhost:8088` | `/health`；等待后端 healthy | 已核验 main CI `PASS`；本分支 `build:real` `PASS` |

已记录的 `restructure-whole` 本轮本地回归：后端 233 tests 全通过；Domain B 现有微服务 20 tests 全通过；前端生产构建成功，2421 modules transformed。本分支没有重跑完整 Compose/Chromium，因此不能用构建通过替代新的本分支全栈运行证明。

## 5. 用例运行情况（总体）

| Domain | 用例范围 | Playwright 入口 | 仓库内逐 UC JSON | 已核验 main CI |
|---|---:|---:|---:|---|
| A：账户与用户治理 | UC01–UC05 | 5/5 | 5/5 | `PASS` |
| B：商品与店铺 | UC06–UC10 | 5/5 | 0/5 | `PASS` |
| C：订单与履约 | UC11–UC15 | 5/5 | 5/5 | `PASS` |
| D：二手交易 | UC16–UC20 | 5/5 | 5/5 | `PASS` |
| E：互动与财务 | UC21–UC25 | 5/5 | 5/5 | `PASS` |
| 合计 | UC01–UC25 | 25/25 | 20/25 | `PASS` |

这里的三种证据不能混写：25/25 spec 是**静态测试入口覆盖**；main 全 UC job 成功是**远端真实 Compose + MySQL + Chromium 执行证据**；20/25 JSON 是**仓库内逐用例原始产物归档完整度**。详细汇总见[测试结果聚合摘要](../test-summary.md)。

## 6. 分用例需求、模型和测试情况

### 6.1 需求与图模型存放规则

四份权威文档已经包含或索引全部需求和图模型，本报告不重复粘贴同一批图：

- 总体用例图、`REQxx / USxx / UCxx`、用例说明、基本/备选/异常流、验收标准和系统级模型：[软件需求说明书](software-requirements.md)；
- 当前架构、组件边界和逐 UC 组件顺序图：[软件概要设计说明书](software-architecture-design.md)；
- 核心类、事务约束和逐 UC 对象顺序图：[软件详细设计说明书](software-detailed-design.md)；
- `需求 → 模型 → 代码 → 测试 → 结果`：[需求追溯矩阵](requirements-traceability-matrix.md)。

每个 `02_docs/UCxx/` 目录同时保留 `requirement.md`、`system.mmd`、`concept.mmd`、`component.mmd`、`component-sequence.mmd`、`object.mmd`、`object-sequence.mmd`、`test-plan.md` 和 `traceability.md`，便于按用例现场检查可编辑源文件。

### 6.2 API/集成测试与 E2E 逐用例表

| 用例 | 最新需求与全部模型入口 | API/集成测试 | 真实浏览器 E2E |
|---|---|---|---|
| UC01 注册、登录和鉴权 | [UC01](../UC01/README.md) | `PASS`：服务、Token/权限分支证据 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC02 资料和地址 | [UC02](../UC02/README.md) | `PASS`：资料、默认地址、越权分支 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC03 商家申请 | [UC03](../UC03/README.md) | `PASS`：申请审批、角色升级与通知 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC04 封禁解禁审计 | [UC04](../UC04/README.md) | `PASS`：封禁、解禁、登录阻断和审计 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC05 举报拉黑信用 | [UC05](../UC05/README.md) | `PASS`：举报审核、拉黑权限和信用分 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC06 搜索筛选详情 | [UC06](../UC06/README.md) | `PASS`：搜索详情及筛选/异常路径 | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` |
| UC07 卖家商品生命周期 | [UC07](../UC07/README.md) | `PASS`：API 场景及服务规则证据 | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` |
| UC08 店铺设置装修 | [UC08](../UC08/README.md) | `PASS`：店铺 API 主流程和权限场景 | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` |
| UC09 商品风险审核 | [UC09](../UC09/README.md) | `PASS`：审核 API、通过/驳回及确定性降级 | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` |
| UC10 浏览搜索热词 | [UC10](../UC10/README.md) | `PASS`：行为 API、历史与热词场景 | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` |
| UC11 购物车结算拆单 | [UC11](../UC11/README.md) | `PASS`：结算、拆单与库存规则 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC12 支付取消 | [UC12](../UC12/README.md) | `PASS`：支付、取消、库存恢复和幂等 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC13 发货物流收货 | [UC13](../UC13/README.md) | `PASS`：发货、轨迹、收货和自动确认 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC14 退款退货仲裁 | [UC14](../UC14/README.md) | `PASS`：申请、仲裁、退款拆分及超时 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC15 评价追评回复 | [UC15](../UC15/README.md) | `PASS`：首评、追评、回复及非法操作 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC16 二手发布管理 | [UC16](../UC16/README.md) | `PASS`：字段校验、本人管理和 MySQL 集成 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC17 二手直接购买 | [UC17](../UC17/README.md) | `PASS`：禁止自购、建单和状态一致性 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC18 二手议价 | [UC18](../UC18/README.md) | `PASS`：同意、拒绝和重复处理 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC19 二手拍卖 | [UC19](../UC19/README.md) | `PASS`：低价/超时拒绝、结算幂等 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC20 二手履约 | [UC20](../UC20/README.md) | `PASS`：二手卖家发货、物流与收货 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC21 优惠券生命周期 | [UC21](../UC21/README.md) | `PASS`：卖家/管理员创建、编辑、关闭 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC22 领券核销 | [UC22](../UC22/README.md) | `PASS`：领取、门槛、结算核销 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC23 钱包账户结算 | [UC23](../UC23/README.md) | `PASS`：充值、账户隔离、结算和流水 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC24 会话消息 | [UC24](../UC24/README.md) | `PASS`：会话创建、消息持久化和权限 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |
| UC25 通知实时推送 | [UC25](../UC25/README.md) | `PASS`：列表、已读、Token 和推送场景 | `LOCAL_E2E_PASS`，1/1；main CI `PASS` |

“API/集成测试 `PASS`”表示相应 UC 已有执行记录，不等于接口清单中的全部公开路由已逐项统计覆盖率。课程要求的“所有公开接口主流程、备选流程和异常流程”目前通过用例维度测试和 Domain 门禁验证；**全公开路由与测试用例的一一覆盖率尚无独立自动审计报告，应作为下一步测试治理项**。

UC14、UC15 曾有后续重试在进入 Playwright 前因外部镜像/环境问题未运行；这不覆盖已经归档的 1/1 通过结果，也不能把失败重试写成新的通过证据。逐次记录见对应 `test-result.md`。

## 7. 四份权威文档内容

| 文档 | 中期检查内容 | 当前状态 |
|---|---|---|
| [软件需求说明书](software-requirements.md) | 统一编号规则、参与者和总体用例图；25 组用户故事、用例说明、前后置条件、基本/备选/异常流、特殊需求、验收标准；逐 UC 系统行为图和概念类图入口 | `PASS` |
| [软件概要设计说明书](software-architecture-design.md) | 当前单体与目标架构、组件边界、逐 UC 组件结构和组件顺序模型、跨组件失败处理 | `PASS` |
| [软件详细设计说明书](software-detailed-design.md) | 核心类图、状态与事务约束、逐 UC 对象/类模型和对象顺序图、异常幂等日志和定时任务 | `PASS` |
| [需求追溯矩阵](requirements-traceability-matrix.md) | 25 个用例的需求编号、模型编号、代码位置、测试编号、结果与缺口 | `PASS` |

图模型统一采用 [diagram-conventions.md](../diagram-conventions.md) 的规范；命名统一采用 [traceability-conventions.md](../traceability-conventions.md) 的 `REQxx / USxx / UCxx / ACxx / *-TCxx` 体系。历史材料在 `02_docs/archive/`，不再维护当前状态。

## 8. CI/CD 构建、测试和部署

流水线定义：[ci-cd.yml](../../.github/workflows/ci-cd.yml)；说明：[PIPELINE.md](../../.github/PIPELINE.md)。

| 阶段 | 自动化内容 | 已记录结论 |
|---|---|---|
| 前端 | `npm ci`、`build:real`、上传 `frontend-dist` | `PASS` |
| 后端 | JWT 契约、Maven 测试、打包并上传 JAR | `PASS` |
| Domain A–E | 复用工作流执行五域 API/集成测试并上传结构化证据 | A、B、C、D、E 均 `PASS` |
| E2E 覆盖门禁 | 要求 UC01–UC25 各有且位于正确 Domain 的 Playwright spec | 25/25 `PASS` |
| 全栈 E2E | 下载已验证前后端制品，启动 Compose/MySQL，先 smoke，再运行全部 Domain Playwright | `PASS` |
| Helm | lint 和 template 校验生产 chart | `PASS` |
| 镜像发布 | 仅 main 通过全部门禁后，以不可变 `sha-<Git SHA>` 推送前后端镜像 | 已记录 main job `PASS` |
| K3s 部署 | Helm `--atomic --wait` 部署并检查公开健康端点 | 已记录 main job `PASS` |
| GitHub Release | 仅 `v*` 标签触发 | 本次非发布触发，`SKIPPED` |

已核验 main run：[33185345952](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952)。五域 job 分别为 [A](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896985040)、[B](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984854)、[C](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984984)、[D](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984973)、[E](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984886)。`restructure-whole` 尚未推送，因此没有该分支专属 GitHub Actions 结果。

## 9. 微服务划分、接口清单和数据库归属

### 9.1 微服务划分

微服务划分图和依赖已经完整收录在[微服务划分与依赖设计](../architecture/microservice-boundaries.md#4-目标关系图)，本报告不重复放图。目标边界为“5 个交付域、6 个业务微服务、1 个入口和 1 个支撑组件”：

| 目标业务服务 | 主要 UC | 当前状态 |
|---|---|---|
| `identity-governance-service` | UC01–UC05 | `TARGET / NOT_IMPLEMENTED` |
| `catalog-shop-service` | UC06–UC10 | Domain B 四个独立迁移原型已实现；目标统一部署单元仍为 `TARGET` |
| `order-service` | UC11–UC15、UC20 | `TARGET / NOT_IMPLEMENTED` |
| `secondhand-service` | UC16–UC19 | `TARGET / NOT_IMPLEMENTED` |
| `benefits-finance-service` | UC21–UC23 | `TARGET / NOT_IMPLEMENTED` |
| `messaging-service` | UC24–UC25 | `TARGET / NOT_IMPLEMENTED` |

API Gateway/Nginx 和 Media adapter 不计入业务微服务数量；`security-contract` 是共享认证契约库，也不是可部署业务服务。每个目标服务只有具备独立 module/JAR、Dockerfile/镜像、Helm Deployment/Service、schema/账号、契约/API/MySQL/恢复测试后，才能改为 `IMPLEMENTED_MICROSERVICE`。

### 9.2 服务接口清单

[服务 API 清单](../architecture/service-api-catalog.md)按当前 Controller、现有微服务 Controller 和 WebSocket 配置盘点公开接口，并冻结迁移后的唯一 owner。当前清单包含约 100 个接口记录行；组合方法展开后对应更多路由。公开路径继续使用 `/api/**`，内部契约使用 `/internal/**`；受保护接口使用 Bearer JWT，客户端自报的 `X-User-Id` 等身份头不可信。

清单还定义了库存预留、结算报价/支付/退款、二手成交建单、身份状态传播、通知等内部 API/事件契约，以及超时、幂等、重试、DLQ、outbox 和补偿策略。**接口归属已设计，不代表这些接口已经从单体迁移到 6 个独立服务。**

### 9.3 数据库归属

[数据库表归属方案](../architecture/database-ownership.md)冻结了单体 33 张逻辑业务表的唯一目标 owner：

| Owner/schema | 主要数据 |
|---|---|
| `identity-governance / identity_governance_db` | 用户、地址、商家申请、举报拉黑、信用和审计 |
| `catalog-shop / catalog_shop_db` | 分类、商品、店铺、风险审核、浏览与搜索行为 |
| `order / order_db` | 订单、明细、售后、评价和物流 |
| `secondhand / secondhand_db` | 二手商品、议价、拍卖和出价日志 |
| `benefits-finance / benefits_finance_db` | 优惠券、用户券、余额和资金流水 |
| `messaging / messaging_db` | 会话、消息和通知 |

冻结规则是一张业务表只有一个写入服务；其他服务只保存稳定 ID/快照或通过版本化 API、事件读取；禁止跨 schema 外键、JOIN、共享 Mapper/Repository；资金余额与流水必须在同一 owner 的本地事务内更新。当前主运行路径仍是单体 MySQL schema，因此表归属为 `PASS（设计）`，物理拆库、账号最小权限和跨库拒绝测试仍为 `NOT_RUN`。

## 10. 当前缺口与后续处理

| 优先级 | 缺口 | 中期表述 | 后续验收条件 |
|---|---|---|---|
| P0 | UC06–UC10 未在仓库归档逐 UC Playwright JSON | `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING` | 逐 UC 保存结果 JSON、截图/trace 和环境信息 |
| P0 | 6 个目标业务服务未全部独立实现部署 | `TARGET / NOT_IMPLEMENTED` | 各服务独立构建、测试、镜像、Helm、schema 和恢复证据 |
| P1 | 公开 API 清单尚无逐路由测试覆盖率门禁 | `PARTIAL` | 自动建立 route → API test 映射并阻断缺口 |
| P1 | 本分支未重跑 Compose 全 UC 浏览器套件 | `NOT_RUN` | 推送分支并取得分支 CI，或本地完整 Compose 运行产物 |
| P1 | 6 schema 最小权限与跨库拒绝测试未执行 | `NOT_RUN` | 空 schema migration、账号权限和拒绝跨 schema 查询证据 |

## 11. 现场复现命令

```powershell
# 文档与测试入口门禁
node scripts/docs/validate-midterm-docs.mjs
node scripts/ci/verify-uc-e2e-coverage.mjs

# 本地构建与 API/集成测试
Set-Location backend
mvn test
Set-Location ..\microservices
mvn test
Set-Location ..\frontend
npm.cmd run build:real

# 全栈启动（仓库根目录）
Set-Location ..
docker compose up --build -d
docker compose ps
```

现场应进一步访问 `http://localhost:8088`，检查 `http://localhost:8089/actuator/health`，并根据 [04_tests/README.md](../../04_tests/README.md) 运行真实 Compose Playwright。运行完成后必须保留命令、环境、开始/结束时间、总数、通过数、失败数、失败原因和原始产物；未执行时保持 `NOT_RUN`。

## 12. 中期交付结论

当前目录顺序和内容已经覆盖助教要求，并额外补齐了基线、证据分层、复现与缺口。提交时以本报告作为文字入口，以四份权威文档作为正式工程文档，以 `02_docs/UC01`–`UC25`、`04_tests/`、`03_devops/` 和 `02_docs/architecture/` 作为可追溯证据。

可准确陈述为：**25 个最新用例已建立完整需求/图模型/追溯和规范的 E2E 入口；已核验 main CI 的全 UC Compose E2E、镜像发布和 K3s 部署通过；本分支构建与 API/集成回归通过。**同时必须附带：**本分支全 Compose E2E 未重跑，UC06–UC10 缺逐 UC本地产物，目标 6 微服务尚未全部落地。**
