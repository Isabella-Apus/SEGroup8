# Kinda Goods 测试结果聚合摘要

> 基线：main `09db0eed`，2026-08-28。状态按“测试入口、仓库内原始证据、远端 CI”分层，不用静态文件存在替代运行通过。

## Domain 汇总

| Domain | UC 范围 | 静态 Playwright 入口 | 仓库内逐 UC Playwright JSON | 最新 main CI |
|---|---:|---:|---:|---|
| A | UC01–UC05 | 5/5 | 5/5 | PASS |
| B | UC06–UC10 | 5/5 | 0/5；服务/API 原始结果存在 | PASS |
| C | UC11–UC15 | 5/5 | 5/5 | PASS |
| D | UC16–UC20 | 5/5 | 5/5 | PASS |
| E | UC21–UC25 | 5/5 | 5/5 | PASS |

最新 main CI：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952>。其中后端、前端、Domain A–E、UC01–UC25 覆盖门禁、全 UC Playwright、镜像发布和 K3s 部署 jobs 成功；Release job 因非发布触发而 skipped。

## 结论

- 25/25 用例均有规范命名的 Playwright spec；这是静态覆盖 PASS。
- 最新 main 的“Playwright smoke and all UC E2E tests” job 成功；这是远端 CI E2E PASS。
- UC01–UC05、UC11–UC25 在仓库内各有 1/1 通过的 Playwright JSON；这是逐 UC 本地证据归档 PASS。
- UC06–UC10 缺少仓库内逐 UC Playwright JSON，状态为 `CI_E2E_PASS / LOCAL_ARTIFACT_MISSING`，不能写成本地证据完整。
- 微服务完成度与用例回归是不同维度。Domain B 四个原型模块存在，但六个目标服务仍未全部独立实现、构建和部署。

## restructure-whole 本轮回归

- 后端：`mvn test`，233/233 通过。
- Domain B 现有微服务：`mvn test`，20/20 通过。
- 前端：`npm.cmd run build:real`，构建成功，2421 modules transformed。
- Compose 全 UC Playwright：本分支 `NOT_RUN`；沿用最新 main CI 结论，不生成伪本地证据。
