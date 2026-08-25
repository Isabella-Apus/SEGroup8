# Kinda Goods 购物与二手交易平台

这是一个面向校园/社区场景的全栈交易平台，包含官方商城、个人闲置交易、卖家工作台和后台管理端。前端使用 Vue 3 + Vite + Pinia + Element Plus，后端使用 Spring Boot 3 + MyBatis-Plus + MySQL。

项目当前的核心设计是：用户入口可以统一，但业务规则在结算和订单侧保持区分。新品订单和二手订单不会混在同一个订单里，统一购物车在结算时会自动拆分为官方商城订单和个人闲置订单。

## 功能概览

### 用户端

- 首页商品推荐、商品搜索、分类筛选、商品详情。
- 官方商城商品浏览、购物车、下单、订单详情、售后与优惠券。
- 二手商城商品浏览、发布闲置、二手详情、二手购物车、二手订单、议价/拍卖相关流程。
- 统一购物车入口 `/cart`：
  - 新品商品合并为官方商城订单。
  - 二手商品按商品/卖家拆分为个人闲置订单。
- 地址管理、个人资料、浏览记录、消息通知、信用中心、评价记录、常见问题。

### 卖家端

- 官方卖家工作台。
- 商品管理、商品发布与编辑。
- 订单管理、财务概览、优惠券/代金券管理。
- 评价管理、消息通知、店铺设置、店铺装修、经营看板、账号健康。

当前卖家工作台主要服务官方商城卖家，二手商品发布入口保留在用户端的闲置发布流程中。

### 管理端

- 用户管理。
- 商家入驻审核。
- 订单管理。
- 举报/报表处理。
- 审计日志。
- 商品风险审核，支持接入大模型进行内容风险判断。

### 后端能力

- JWT 登录鉴权。
- MyBatis-Plus 数据访问。
- 订单、支付、退款、售后、物流提醒与自动确认收货。
- 官方商品与二手商品分类型订单处理。
- 文件上传。
- WebSocket 实时消息。
- 幂等请求处理。
- Swagger/OpenAPI 接口文档。
- 商品风险审核大模型配置。

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- ECharts
- SortableJS / VueDraggable
- Fuse.js

### 后端

- Java 17
- Spring Boot 3.3.4
- MyBatis-Plus 3.5.7
- MySQL 8
- Maven
- JWT
- SpringDoc OpenAPI
- H2 测试数据库

## 目录结构

```text
SEGroup8
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/       # 业务代码
│   ├── src/main/resources/  # 配置、SQL、静态资源
│   ├── src/test/java/       # 后端测试
│   ├── start.ps1            # 常规启动脚本
│   ├── start-reset.ps1      # 重置数据库启动脚本
│   └── pom.xml
├── frontend/                # Vue 3 前端
│   ├── src/api/             # API 封装
│   ├── src/assets/          # 静态资源
│   ├── src/components/      # 公共组件
│   ├── src/layout/          # 用户端/卖家端/管理端布局
│   ├── src/mock-data/       # 前端 mock 数据
│   ├── src/router/          # 路由配置
│   ├── src/stores/          # Pinia 状态管理
│   ├── src/utils/           # 工具方法
│   ├── src/views/           # 页面视图
│   └── package.json
├── sql/                     # 数据库脚本
├── DEPLOY_ALIYUN.md         # 阿里云部署说明
├── SECURITY.md              # 安全说明
└── README.md
```

## 环境要求

- Node.js 18 或更高版本
- npm
- JDK 17
- Maven 3.8 或更高版本
- MySQL 8

## 快速启动

### Docker Compose 一键启动（Issue #65）

已提供前端、后端和 MySQL 的完整容器化编排。Docker Desktop 启动后，在仓库根目录执行：

```powershell
docker compose -f compose.yml up -d --build
```

默认前端地址为 `http://127.0.0.1:8088`，后端健康检查为 `http://127.0.0.1:8089/actuator/health`，MySQL 映射端口为 `3307`。完整说明与验收命令见 [DOCKER.md](DOCKER.md)。

### 1. 克隆项目

```bash
git clone <repository-url>
cd SEGroup8
```

### 2. 配置后端本地环境

后端默认会读取 `backend/src/main/resources/application.yml`，并额外尝试加载本地配置文件：

```text
backend/src/main/resources/application-local.yml
```

首次启动前可以复制示例配置：

```powershell
cd backend
Copy-Item src/main/resources/application-local.example.yml src/main/resources/application-local.yml
```

然后根据本机环境修改：

```yaml
spring:
  datasource:
    username: root
    password: your_mysql_password

risk-audit:
  llm:
    api-key: your_llm_api_key
```

大模型审核不是本地启动的必需项。没有密钥时，可以先保留示例值或关闭相关配置。

### 3. 启动后端

推荐使用项目提供的启动脚本：

```powershell
cd backend
.\start.ps1
```

脚本会在本地配置不存在时自动复制 `application-local.example.yml`，并使用 `start-schema` 配置启动后端。

后端默认地址：

```text
http://localhost:8080
```

Swagger 接口文档：

```text
http://localhost:8080/swagger-ui.html
```

如果需要重置全部数据库数据，可以使用：

```powershell
cd backend
.\start-reset.ps1
```

注意：重置脚本会清空并重新初始化数据，执行前会要求输入确认文本。

### 4. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5174
```

如果 5174 端口被占用，Vite 会自动切换到下一个可用端口，请以终端输出为准。

## 前端数据源模式

前端通过 `VITE_DATA_SOURCE` 控制使用真实后端还是 mock 数据。

```text
frontend/.env.development   # 默认真实后端
frontend/.env.mock          # mock 数据
frontend/.env.real          # 真实后端
frontend/.env.production    # 生产环境真实后端
```

常用命令：

```bash
npm run dev        # 开发模式，默认读取 .env.development
npm run dev:mock   # 使用 mock 数据
npm run dev:real   # 使用真实后端
npm run build      # 生产构建
npm run build:mock # mock 模式构建
npm run build:real # 真实后端模式构建
```

如果页面没有请求到真实数据，优先检查：

- 后端是否运行在 `http://localhost:8080`。
- 当前前端环境是否为 `VITE_DATA_SOURCE=real`。
- 浏览器控制台和 Network 面板中是否存在接口错误。

## 主要路由

### 用户端

```text
/                         首页
/login                    登录
/register                 注册
/product                  官方商城
/product/:id              官方商品详情
/secondhand               二手商城
/secondhand/:id           二手商品详情
/secondhand/publish       发布闲置
/cart                     统一购物车
/order                    新品订单
/secondhand/orders        二手订单
/profile                  个人资料
/addresses                地址管理
/browse-history           浏览记录
/messages                 消息
/notifications            通知
```

### 卖家端

```text
/merchant                 卖家工作台
/merchant/orders          卖家订单
/merchant/finance         财务管理
/merchant/reviews         评价管理
/merchant/shop            店铺设置
/merchant/seller-products 商品管理
/merchant/vouchers        优惠券管理
/merchant/account-health  账号健康
```

### 管理端

```text
/admin                    管理首页
/admin/users              用户管理
/admin/merchant-review    商家审核
/admin/orders             订单管理
/admin/reports            举报与报表
/admin/audit-logs         审计日志
/admin/product-risk-audits 商品风险审核
```

## 订单与购物车规则

当前项目区分两类商品：

- 新品：官方商城商品，通常由商家统一发货，支持多个商品合并下单。
- 二手：个人闲置商品，通常库存为 1，更适合按商品和卖家拆分订单。

因此，购物车入口可以统一，但结算时会自动分组：

```text
官方商城订单
- 键盘
- 耳机

个人闲置订单
- 自行车，卖家 A

个人闲置订单
- 教材，卖家 B
```

订单列表也按类型区分：

- `/order` 只展示新品订单。
- `/secondhand/orders` 只展示二手订单。

## 常用开发命令

### 前端

```bash
cd frontend
npm install
npm run dev
npm run build
```

### 后端

```bash
cd backend
mvn spring-boot:run
mvn -DskipTests compile
mvn test
```

如果直接使用 Maven 启动，需要确认本地数据库和配置文件已经准备好。日常开发更推荐使用 `start.ps1`。

## 数据库说明

默认数据库连接配置位于：

```text
backend/src/main/resources/application.yml
```

默认数据库名：

```text
segroup8_platform
```

建议不要直接修改 `application.yml` 中的公共配置，而是在本地创建：

```text
backend/src/main/resources/application-local.yml
```

这个文件用于保存本机数据库密码、API Key 等私有配置，并且不应提交到 Git。

## 文件上传

后端默认上传目录为：

```text
backend/uploads
```

上传目录属于运行时数据，不建议提交到版本库。

## 实时消息

后端提供 WebSocket 实时通道：

```text
/ws/realtime
```

本地开发默认允许 `localhost` 和局域网地址访问。

## 商品风险审核

商品风险审核配置位于后端配置文件的 `risk-audit.llm` 节点。项目支持通过环境变量注入密钥：

```text
RISK_AUDIT_LLM_API_KEY
OPENAI_API_KEY
```

本地没有密钥时，可以先使用普通商品流程开发；涉及风险审核的能力需要补充有效 API Key。

## Git 忽略项

项目会忽略常见运行时和本地配置文件，例如：

- `node_modules/`
- `dist/`
- `target/`
- `backend/uploads/`
- `backend/src/main/resources/application-local.yml`
- `.playwright-mcp/`

其中 `.playwright-mcp/` 是 Playwright MCP 工具生成的页面快照目录，只用于本地浏览器调试，不属于业务代码。

## 常见问题

### 前端页面没有数据

先确认当前是否使用真实后端模式：

```bash
npm run dev:real
```

然后检查后端是否已经启动：

```text
http://localhost:8080
```

### 登录或接口请求失败

检查以下几项：

- MySQL 服务是否启动。
- `application-local.yml` 中账号密码是否正确。
- 后端控制台是否有 SQL 或鉴权错误。
- 前端当前是否连接真实后端。

### 想使用 mock 数据开发

使用：

```bash
cd frontend
npm run dev:mock
```

mock 数据位于：

```text
frontend/src/mock-data
```

### 数据库需要重新初始化

使用重置脚本：

```powershell
cd backend
.\start-reset.ps1
```

执行前请确认本地数据可以被清空。

## 交付与构建

前端生产构建：

```bash
cd frontend
npm run build
```

后端编译：

```bash
cd backend
mvn -DskipTests compile
```

云服务器部署可以参考：

```text
DEPLOY_ALIYUN.md
```

## 项目定位

本项目不是单纯的商品展示页面，而是一个包含用户端、卖家端、管理端、官方商品交易和个人闲置交易的综合平台。后续扩展时建议继续保持以下边界：

- 用户入口可以统一。
- 新品和二手的订单、库存、发货、售后规则需要保持区分。
- 卖家端主要面向官方商城商家。
- 个人闲置发布和管理更适合保留在用户端流程中。
