# 购物与二手交易平台基础工程

本项目是一个前后端分离的课程项目基础框架，适合作为“购物与二手交易平台”的开发起点。当前重点是把工程跑起来，并提供统一的目录结构、公共能力和基础联调链路，方便后续多人并行开发。

## 1. 项目结构

```text
SEGroup8/
  backend/                 # Spring Boot 后端
  frontend/                # 当前前端
    src/mock-data/         # 统一测试数据生成与接口模拟
  sql/
    init.sql               # 手动初始化数据库脚本
```

## 2. 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus

### 后端

- Java
- Spring Boot
- Spring MVC
- MyBatis-Plus
- MySQL
- JWT
- Swagger / OpenAPI

## 3. 环境准备

启动项目之前，请先安装这些依赖。

### 3.1 安装 Node.js

前端运行需要 Node.js。

建议版本：

- Node.js 18 及以上

官网下载：

- <https://nodejs.org/>

安装完成后，在命令行输入：

```bash
node -v
npm -v
```

如果能看到版本号，说明安装成功。

### 3.2 安装 Java

后端使用 Spring Boot，要求 JDK 17 及以上。

建议版本：

- JDK 17 或更高

安装完成后，在命令行输入：

```bash
java -version
```

如果能看到 Java 版本号，说明安装成功。

### 3.3 安装 Maven

后端使用 Maven 管理依赖。

#### 下载 Maven

Maven 官网：

- <https://maven.apache.org/download.cgi>

建议下载：

- `Binary zip archive`

#### 安装 Maven

1. 下载 zip 包
2. 解压到本地目录，例如：

```text
C:\Users\你的用户名\tools\apache-maven-3.9.11
```

1. 配置环境变量：

- 新建用户变量或系统变量：`MAVEN_HOME`
- 值填写 Maven 解压目录，例如：

```text
C:\Users\你的用户名\tools\apache-maven-3.9.11
```

- 在 `Path` 中追加：

```text
%MAVEN_HOME%\bin
```

1. 关闭当前终端，重新打开一个新的终端窗口

#### 验证 Maven

```bash
mvn -version
```

如果能看到 Maven 版本和 Java 版本，说明 Maven 安装成功。

### 3.4 安装 MySQL

后端需要 MySQL。

建议版本：

- MySQL 8.x

请确认：

- MySQL 服务已经启动
- 你知道数据库账号和密码

如果你的电脑没有 `mysql` 命令，但安装了 MySQL Shell，可以使用：

```bat
"C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root@localhost -p
```

## 4. 前后端依赖怎么下载

### 4.1 前端依赖怎么下载

进入前端目录后执行：

```powershell
cd frontend
npm install
```

这条命令会根据 [package.json](/c:/Users/34267/Desktop/code/SEGroup8/frontend/package.json) 自动下载前端依赖，包括：

- vue
- vite
- vue-router
- pinia
- axios
- element-plus

### 4.2 后端依赖怎么下载

进入后端目录后执行：

```powershell
cd backend
mvn clean install
```

或者直接：

```powershell
mvn spring-boot:run
```

Maven 会根据 [pom.xml](/c:/Users/34267/Desktop/code/SEGroup8/backend/pom.xml) 自动下载后端依赖，包括：
Maven 会根据 `backend/pom.xml` 自动下载后端依赖，包括：

- spring-boot-starter-web
- spring-boot-starter-validation
- mybatis-plus-spring-boot3-starter
- mysql-connector-j
- lombok
- jjwt
- springdoc-openapi

## 5. 数据库账号和密码在哪里填写

公共配置文件在：

- `backend/src/main/resources/application.yml`

请不要在公共配置里填写个人数据库密码。

### 5.1 本地私有配置（推荐）

在下面路径创建本地文件（该文件已被 `.gitignore` 忽略）：

- `backend/src/main/resources/application-local.yml`

可选：复制示例文件作为模板（示例仅用于参考）：

- `backend/src/main/resources/application-local.example.yml`

示例内容：

```yml
spring:
  datasource:
    username: root
    password: 你的MySQL密码
```

如果你的账号不是 `root`，请改成你自己的账号。

### 5.2 自动创建（脚本启动）

如果你使用下面脚本启动后端：

- `backend/start.bat`
- `backend/start.ps1`

当 `application-local.yml` 不存在时，脚本会自动从 `application-local.example.yml` 复制并创建本地文件。

你只需要修改新生成文件里的这两项：

- `spring.datasource.username`
- `spring.datasource.password`

注意：如果你直接使用 `mvn spring-boot:run`，脚本不会执行，需先手动创建 `application-local.yml`。

注意：

- `application-local.yml` 是本机私有配置，不会提交到仓库。
- `application-local.example.yml` 只保留占位符，不能填写真实密码后提交。

## 6. SQL 初始化与两种启动模式

后端现在区分为两种启动模式：

- 日常测试启动：不强制执行 SQL 初始化，不会自动清空或覆盖已有业务数据。
- 全量初始化启动：会执行“全表清空 + 重新初始化”脚本，适合需要重置环境时使用。

### 6.1 日常测试启动（默认）

默认配置文件：

- [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)

其中已设置：

- `spring.sql.init.mode: never`

这意味着正常启动后端时，不会自动执行 [backend/src/main/resources/schema.sql](backend/src/main/resources/schema.sql) 和 [backend/src/main/resources/data.sql](backend/src/main/resources/data.sql)。

### 6.2 全量初始化启动（清空后重建）

> [!WARNING]
> 高风险操作：该模式会清空当前数据库中的业务数据（包括订单、商品、优惠券、余额流水等）并重建基础数据。
> 请勿在需要保留数据的环境执行。

> [!IMPORTANT]
> 为防误触，`start-reset.ps1` / `start-reset.bat` 已加入二次确认：
> 第一步必须输入 `YES`，第二步必须输入 `RESET-ALL`，任一步不匹配都会立即取消。

专用配置文件：

- [backend/src/main/resources/application-reset-all.yml](backend/src/main/resources/application-reset-all.yml)

专用数据脚本：

- [backend/src/main/resources/data-reset-all.sql](backend/src/main/resources/data-reset-all.sql)

该模式会先清空业务表，再按初始化脚本重建基础数据，请谨慎使用。

### 6.3 手动初始化（可选）

如果你想手动执行 SQL，可以使用：

- [sql/init.sql](sql/init.sql)

你可以在 MySQL 客户端里执行这个文件。

## 7. 默认测试账号

项目自带测试数据，默认账号如下：

- `admin / admin123`
- `seller / seller123`
- `user / user123`

## 8. 怎么启动后端

后端必须在 `backend` 目录下启动。

### 方式一：日常测试启动（推荐，不清库）

#### 1) 使用脚本启动

```powershell
cd backend
powershell -ExecutionPolicy ByPass -File .\start.ps1
```

或：

```bat
cd backend
start.bat
```

#### 2) 使用 Maven 启动

```powershell
cd backend
mvn spring-boot:run
```

### 方式二：全量初始化启动（会清空并重建数据）

#### 1) 使用 reset 脚本启动

```powershell
cd backend
powershell -ExecutionPolicy ByPass -File .\start-reset.ps1
```

执行后会出现二次确认：

1. Step 1/2: 输入 `YES`
2. Step 2/2: 输入 `RESET-ALL`

任一步输入不匹配，脚本会直接退出，不会执行清库。

或：

```bat
cd backend
start-reset.bat
```

#### 2) 使用 Maven 指定 profile 启动

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=reset-all
```

### 方式三：运行打包好的 jar（不清库）

```powershell
cd backend
java -jar target\platform-backend-0.0.1-SNAPSHOT.jar
```

### 后端启动成功标志

如果后端启动成功，日志中会出现类似：

```text
Tomcat started on port 8080
Started PlatformApplication
```

后端访问地址：

- 接口地址：`http://localhost:8080`
- Swagger 地址：`http://localhost:8080/swagger-ui.html`

## 9. 怎么启动前端

前端必须在 `frontend` 目录下启动。

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：

- `http://localhost:5174`

说明：

- `dev:mock` / `dev:real` 已在 `frontend/package.json` 配置。
- 数据源统一入口在 `frontend/src/api/http.js`。
- 测试数据统一放在 `frontend/src/mock-data/`。

## 10. 推荐启动顺序

推荐按这个顺序启动：

1. 启动 MySQL
2. 启动后端
3. 启动前端

## 11. 常见问题

### 11.1 `mvn` 不是内部或外部命令

说明 Maven 没有正确安装，或者当前终端还没刷新环境变量。

处理方法：

1. 确认已经配置 `MAVEN_HOME`
2. 确认 `Path` 里包含 `%MAVEN_HOME%\bin`
3. 关闭当前终端，重新打开后再执行：

```bash
mvn -version
```

### 11.2 `No plugin found for prefix 'spring-boot'`

通常说明你在错误目录执行了命令。

正确方式：

```powershell
cd backend
mvn spring-boot:run
```

不要在 `frontend` 目录执行 `mvn spring-boot:run`。

### 11.6 二手模块报 `No static resource api/secondhand/list`

原因通常是：当前使用 `real` 数据源，但后端尚未提供二手接口。

处理方法：

1. 临时切到 mock：在 `frontend` 下执行 `npm run dev:mock`
2. 或补齐后端二手接口后再使用 `npm run dev:real`

### 11.3 前端报 `AxiosError: Network Error`

这通常不是前端依赖有问题，而是后端没成功启动。

请先检查：

- `http://localhost:8080/swagger-ui.html` 是否能打开
- 后端日志里是否有 `Tomcat started on port 8080`

### 11.4 数据库连接失败

如果看到类似：

```text
Access denied for user 'root'@'localhost'
```

说明 `application.yml` 或 `application-local.yml` 里的数据库账号或密码不对。

请修改：

- `spring.datasource.username`
- `spring.datasource.password`

### 11.5 浏览器报 CORS 错误

如果后端没有真正启动，浏览器经常会表现成 CORS 或 Network Error。

所以遇到 CORS 时，先确认后端是否启动成功，而不是先改前端。

## 12. 当前已完成的基础能力

### 前端

- Vue Router 路由配置
- Pinia 登录状态管理
- Axios 请求封装
- 请求与响应拦截器
- Token 自动携带
- 登录状态持久化
- 用户端、卖家端、管理端布局基础壳子

### 后端

- 统一返回体 `Result`
- 全局异常处理
- JWT 登录鉴权
- 登录拦截器
- CORS 配置
- Swagger / OpenAPI
- 文件上传接口

## 13. 团队后续开发建议

- 用户模块：`frontend/src/views/user`、`backend/controller/user`
- 商品模块：`frontend/src/views/product`、`backend/service/product`
- 订单模块：`frontend/src/views/order`
- 二手模块：`frontend/src/views/secondhand`
- 店铺模块：`frontend/src/views/seller`
- 管理后台模块：`frontend/src/views/admin`

这套基础工程已经适合作为课程项目正式开发起点。
