# UC07 远程材料审计

审计日期：2026-08-28

## 结论

当前 `documents` 分支需要以现有单体实现补齐 UC07 的需求、设计和追溯材料。历史提交 `4b99af1` 的独立 CatalogLifecycle 微服务文件与当前 `ProductController`/`ProductServiceImpl` 边界不一致，因此只作为差异参考，不整体恢复为当前文档或证据。

## 当前事实

- 当前实现入口为 `backend/src/main/java/com/segroup8/platform/controller/ProductController.java` 和 `.../service/impl/ProductServiceImpl.java`。
- 当前状态模型使用 `ON_SHELF/OFF_SHELF`，所有权通过当前用户对应的店铺校验。
- 当前后端单元基线为 `ProductServiceImplTest`，本轮文档矩阵引用其中 5 条测试。
- 浏览器流程文件 `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts` 仍需独立的 Compose/MySQL Playwright 验证；本轮不将其标记为新的 E2E 通过。

## 当前材料入口

- [UC07 需求](../../01_requirements/UC07-卖家商品生命周期.md)
- [UC07 设计](../../02_docs/UC07-卖家商品生命周期-设计.md)
- [UC07 追溯矩阵](../../02_docs/UC07-卖家商品生命周期-追溯矩阵.md)
- [UC07 测试计划](../../04_tests/UC07/test-plan.md)
- [UC07 测试报告](../../04_tests/UC07/test-report.md)

## 历史材料处理规则

`origin/codex/uc07-results` 和提交 `4b99af1` 保留为历史审计来源。不得恢复旧工作流、旧微服务接口或与当前单体代码不匹配的测试作为当前完成证据。后续若补浏览器 E2E，应在统一 Domain-B Compose 工作流中单独产生新证据。
