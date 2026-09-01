# Domain B - 商品、店铺、风险与行为

当前实现：`microservices/catalog-shop-service` 是 UC06-UC10 唯一可构建、镜像化和部署的服务。其源码内按 catalog、shop、risk、behavior 划分模块；旧的四个重复原型目录已移除，避免被 Maven 或部署工具误识别为运行时服务。

| 用例 | 单 UC 文档 | 浏览器测试 | 当前证据 |
|---|---|---|---|
| UC06 | [入口](../../UC06/README.md) | `frontend/e2e/domain-b/uc06-catalog.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC07 | [入口](../../UC07/README.md) | `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC08 | [入口](../../UC08/README.md) | `frontend/e2e/domain-b/uc08-shop.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC09 | [入口](../../UC09/README.md) | `frontend/e2e/domain-b/uc09-risk-audit.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC10 | [入口](../../UC10/README.md) | `frontend/e2e/domain-b/uc10-behavior.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |

Domain B 最新 CI job：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984854>。
