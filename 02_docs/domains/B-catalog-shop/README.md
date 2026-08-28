# Domain B：商品与店铺

Domain B 覆盖 UC06–UC10，负责商品查询、卖家商品生命周期、店铺维护、风控审核和行为记录。

## 用例文档入口

| 用例 | 需求 | 设计 | 追溯 | 当前文档/测试口径 |
| --- | --- | --- | --- | --- |
| UC06 | `01_requirements/UC06-商品目录与搜索.md` | `02_docs/UC06-商品目录与搜索-设计.md` | `02_docs/UC06-商品目录与搜索-追溯矩阵.md` | API/集成证据已有 |
| UC07 | [需求](../../../01_requirements/UC07-卖家商品生命周期.md) | [设计](../../UC07-卖家商品生命周期-设计.md) | [追溯](../../UC07-卖家商品生命周期-追溯矩阵.md) | 后端单元证据已有；浏览器 E2E 待补 |
| UC08 | `01_requirements/UC08-店铺维护.md` | `02_docs/UC08-店铺维护-设计.md` | `02_docs/UC08-店铺维护-追溯矩阵.md` | API/集成证据已有 |
| UC09 | `01_requirements/UC09-商品风控审核.md` | `02_docs/UC09-商品风控审核-设计.md` | `02_docs/UC09-商品风控审核-追溯矩阵.md` | API/集成证据已有 |
| UC10 | `01_requirements/UC10-浏览记录搜索历史与热词.md` | `02_docs/UC10-浏览记录搜索历史与热词-设计.md` | `02_docs/UC10-浏览记录搜索历史与热词-追溯矩阵.md` | API/集成证据已有 |

## 验证口径

- API/集成测试使用 Spring Boot、MockMvc、H2 或现有测试配置，不启动浏览器。
- 浏览器 E2E 必须使用 Compose 前端、后端和 MySQL，不能用 Vite mock 代替。
- UC07 的历史独立微服务材料只作比对依据；当前文档按单体 `ProductController`/`ProductServiceImpl` 重建，避免恢复与当前代码不匹配的旧实现。
- 结果汇总见 [`02_docs/测试结果聚合摘要.md`](../../测试结果聚合摘要.md) 和 [`04_tests/domains/B-catalog-shop/evidence/result-summary.json`](../../../04_tests/domains/B-catalog-shop/evidence/result-summary.json)。
