# 04_tests - 自动化入口与原始证据

本目录不复制测试源码。Java 测试保留在 `backend/src/test` 或各微服务 `src/test`，Playwright 测试保留在 `frontend/e2e`。

根目录只保留以下规范入口：

- `UC01/`–`UC25/`：单用例运行入口、Evidence 和原始报告；
- `domains/`：Domain A–E 的跨用例汇总与运行器；
- `performance/`：k6 等性能脚本、数据和结果；
- `platform-e2e/`：共享全栈 smoke、全量 E2E、Compose 日志与容器验收。

测试计划、测试报告和追溯表统一位于 `../02_docs/UCxx/`。重构前的聚合测试计划、人工走查和阶段性报告保存在 `../05_management/archive/test-assets/`，不作为当前测试状态的事实来源。

CI 临时摘要写入仓库根目录的 `.ci/` 并作为 GitHub Actions Artifact 上传，不在 `04_tests/` 中长期保存。新的测试资产不得在本目录根级新增松散文件或额外的 `UCxx-UCyy/` 聚合目录。
