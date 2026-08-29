# Domain B - 商品、店铺、风险与行为

当前实现：`catalog-service`、`shop-service`、`risk-service`、`behavior-service` 四个可独立构建/测试的原型模块。目标 `catalog-shop-service` 尚未作为单一独立部署单元完成。

| 用例 | 单 UC 文档 | 浏览器测试 | 当前证据 |
|---|---|---|---|
| UC06 | [入口](../../UC06/README.md) | `frontend/e2e/domain-b/uc06-catalog.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC07 | [入口](../../UC07/README.md) | `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC08 | [入口](../../UC08/README.md) | `frontend/e2e/domain-b/uc08-shop.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC09 | [入口](../../UC09/README.md) | `frontend/e2e/domain-b/uc09-risk-audit.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |
| UC10 | [入口](../../UC10/README.md) | `frontend/e2e/domain-b/uc10-behavior.spec.ts` | CI E2E PASS；仓库缺逐 UC Playwright JSON |

Domain B 最新 CI job：<https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98896984854>。
