# 04_tests - 自动化入口与原始证据

本目录不复制测试源码。Java 测试保留在 `backend/src/test` 或各微服务 `src/test`，Playwright 测试保留在 `frontend/e2e`。

- `UC01`–`UC25`：单用例运行入口、Evidence 和原始报告；
- `domains/`：跨用例 Domain 汇总与运行器；
- `performance/`：k6 等性能脚本和结果；
- `platform-e2e/`：共享全栈冒烟证据。

测试计划、测试报告和追溯表统一位于 `../02_docs/UCxx/`。
