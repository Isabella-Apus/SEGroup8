# 需求追溯

| UC | 实现 | 自动测试 | E2E |
|---|---|---|---|
| UC06 | category tree、product list/search/detail | `CatalogShopApiTest.catalogEndpoints` | `frontend/e2e/domain-b/uc06-catalog.spec.ts` |
| UC07 | seller create/update/off-shelf、审核触发、所有权 | `sellerLifecycleAndOwnership` | `uc07-product-lifecycle.spec.ts` |
| UC08 | public/seller shop、装修、商家事件幂等 | `shopAndMerchantEventAreIdempotent` | `uc08-shop.spec.ts` |
| UC09 | 确定性降级、人工决策、outbox | `deterministicRiskFallbackAndDecision` | `uc09-risk-audit.spec.ts` |
| UC10 | 用户隔离的浏览/搜索、热词 | `behaviorIsUserScopedAndCountsHotKeywords` | `uc10-behavior.spec.ts` |
| 库存协作 | reserve/get/confirm/release/expire | `inventoryIsAtomicIdempotentAndStateful` | order 合约测试 |
