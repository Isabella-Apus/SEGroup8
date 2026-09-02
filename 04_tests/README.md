# 04_tests 测试结果与实验数据

测试代码保留在源码树：Java 测试位于 `backend/src/test/` 和各微服务 `src/test/`，Playwright 位于 `frontend/e2e/`。本目录只保存交付所需的结构化结果、关键截图和可复现实验数据。

- `UC01/` 至 `UC25/`：逐用例 JSON/XML 和关键成功截图；
- `microservices/`：六个服务的 API、真实数据库、独立 E2E 和部署测试摘要；
- `platform-e2e/`：完整系统 Compose/Playwright 入口；
- `domains/B-catalog-shop/`、`domains/C-order-fulfillment/`：仍由 CI 使用的领域测试运行器；
- `cloud-native-experiments/20260830-2300-ef71f1fe/performance/`：改造前后 3 个接口、每种架构各 3 轮原始数据；
- `cloud-native-experiments/20260902-system-hpa-b622e6bb/`：最终完整系统 HPA 数据；
- `cloud-native-experiments/20260902-order-fault-b622e6bb/`：最终 Order 依赖故障与恢复数据。

完整 Playwright HTML、trace、video 和流水线日志不长期提交 Git，由 Actions artifact 保存。测试结论不检查说明文档或文件 hash；验收依据是实际代码执行、数据库集成、浏览器流程和部署结果。
