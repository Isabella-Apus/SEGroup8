# [PLATFORM][E2E] 建立统一 Playwright + Compose 真实 E2E 脚手架


## Task metadata

- Owner：成员 A
- Parent：团队流水线 Issue #66
- Type：Platform / Shared Infrastructure
- Scope：服务 UC01-UC25 的共同 E2E 前置能力
- Estimate：2 人日（实现与校验）
- Reviewer：非作者成员（重点审查真实 Compose 链路、证据和可复用性）
- PR：PR-D0，标题为 test(platform): bootstrap Playwright Compose full-stack E2E
- 完成后 PR 链接：创建 PR 后回填；未创建前不得填写虚假链接

## 当前缺口

仓库已有 Compose、前端/后端容器、MySQL seed 和 HTTP health endpoint，但没有
统一 Playwright 配置、共享 fixture/helper、浏览器 smoke、Compose 分阶段等待、
失败诊断 artifact 或统一真实 E2E CI job。既有 Issue #65 evidence 只验证容器、
接口和数据库，不替代真实浏览器 E2E。

## 影响范围

这是 UC01-UC25 后续真实浏览器 E2E 的平台前置任务，不计作任何单个 UC 的
E2E 证据。Domain A-E 可以并行准备 API、fixture、spec 初稿和追溯，但必须在
本 Task 合并后用本入口在真实 Compose 上执行，才能把 E2E_PENDING 改为 PASS。

## 修改文件

- frontend/playwright.config.ts
- frontend/e2e/fixtures/
- frontend/e2e/helpers/
- frontend/e2e/smoke/full-stack.smoke.spec.ts
- frontend/package.json / frontend/package-lock.json
- frontend/src/views/user/Login.vue（仅添加稳定的登录测试选择器）
- scripts/e2e/run-compose-e2e.ps1
- scripts/e2e/run-compose-e2e.sh
- .env.docker.example / .gitignore
- .github/workflows/ci-cd.yml
- 04_tests/platform-e2e/README.md

不包含 UC16-UC20 或 A/B/C/E 的具体业务 spec，不创建第二套 Playwright、
Cypress、Vite mock E2E 或 UC workflow。

## 验收条件

- [ ] 全队只有 frontend/playwright.config.ts 一套 Playwright 配置。
- [ ] 统一脚本可 build 并按 database → backend → frontend 顺序启动 Compose。
- [ ] 使用 Docker healthcheck、后端/前端 HTTP health 和有超时的主动轮询。
- [ ] 浏览器访问 Compose 中真实 Nginx 前端，前端经真实 API 读取 MySQL seed 数据。
- [ ] full-stack smoke 有登录、API 响应和页面商品断言。
- [ ] 失败保存 HTML report、JSON result、截图、trace、video 和前后端/MySQL 日志。
- [ ] CI 上传证据并返回 Playwright 真实退出码，失败阻断 deploy/release。
- [ ] 支持 Domain 目录、单 spec、grep 定向运行和无参数全量运行。
- [ ] 没有提交个人密码、token、私钥或生产 Secret。
- [ ] 本文档和共享 DevOps 运行说明完整，合并后由非作者 Review。

## 本地运行命令

~~~powershell
Copy-Item .env.docker.example .env
.\scripts\e2e\run-compose-e2e.ps1
~~~

定向执行：

~~~powershell
.\scripts\e2e\run-compose-e2e.ps1 e2e/domain-a/
.\scripts\e2e\run-compose-e2e.ps1 e2e/domain-a/uc01-login.spec.ts
.\scripts\e2e\run-compose-e2e.ps1 --grep '@UC01'
~~~

## CI 运行命令

~~~bash
scripts/e2e/run-compose-e2e.sh
~~~

CI job 在前端 build 和后端 automated tests 成功后执行；失败不允许后续发布
或部署 job 继续。

## Evidence 输出位置

生成到 04_tests/platform-e2e/evidence/：

- playwright-report/：HTML 报告
- playwright-results.json：机器可读结果
- test-results/：失败截图、trace、video
- logs/：Compose status/config、Compose 各服务、启动阶段和 Playwright 日志

## 风险 / 阻塞

- 本地需要 Docker Desktop/Engine、可拉取 Docker 镜像和 npm registry。
- MySQL seed 脚本只在空 volume 首次初始化；写入型 Domain spec 必须隔离或清理
  数据。脚本默认不删除 volume，-ResetDatabase 是明确的可丢弃数据选项。
- 本环境若没有 Git/gh 登录或仓库写权限，只能提交代码准备和本文档，不能声称
  已创建 Issue、分支或 PR；需要人工用权限完成 GitHub 操作并等待非作者 Review。

## Issue body 创建内容

创建 GitHub Sub-issue 时复制本文档的当前缺口、影响范围、修改文件、验收条件、
运行命令、Evidence、Estimate、Reviewer、风险/阻塞和 PR 链接字段；创建前先搜索
同名/同用途 Issue，禁止重复创建。
