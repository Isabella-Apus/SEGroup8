# UC07 Seller Product Lifecycle Test Plan

## Scope

Validate a seller product lifecycle against the real frontend, backend, and MySQL stack:

- create a product with an uploaded image;
- edit its name and verify the seller API returns the persisted record;
- increase stock and reject a negative resulting stock level;
- take the product off sale, delete it, and confirm it is no longer retrievable;
- deny a buyer access to the seller workbench.

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC07-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/ProductServiceImplTest.java#createSellerProduct_shouldPersistWithCurrentSellerShopId`; `#adjustSellerProductStock_shouldThrowWhenResultNegative`; `#pageSellerProducts_shouldUseCurrentSellerShopIdAndReturnPageVO` | 卖家店铺归属、库存下限和卖家工作台隔离。该共享测试类当前未声明 `UC07` tag，按方法语义登记。 |
| `E2E-TC07-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts#creates, edits, shelves, adjusts stock and deletes a product with persistence checks`; `#denies the buyer access to the seller workbench` | 创建、编辑、库存、上下架、删除和买家越权均在真实栈验证；不把 MockMvc 当作 E2E。 |

当前源码未发现独立 UC07 Integration 测试；服务层共享回归和真实浏览器入口如上。

## Commands

```powershell
npm --prefix frontend run build:real
powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

The browser command rebuilds the Compose stack, refreshes the idempotent fixture, and runs the Domain B Playwright suite serially.
