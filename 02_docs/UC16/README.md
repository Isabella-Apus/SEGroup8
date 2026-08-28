# UC16 二手商品发布与管理设计

## 设计范围

本文对应 `UC16`，覆盖卖家发布、编辑、上下架和删除本人二手商品，以及分类、输入边界、所有权和已售状态约束。直接购买、议价、拍卖和履约分别归 UC17-UC20。

## 设计视图

- [系统交互图](system.mmd)
- [组件图](component.mmd)
- [对象图](object.mmd)
- [追溯矩阵](traceability.md)

## 组件职责与归属

| 层级 | 组件 | 职责 |
|---|---|---|
| 前端 | `SecondhandPublishView.vue` | 从真实分类树选择二级分类，上传图片并发布商品 |
| 前端 | `MerchantSecondhandProductsView.vue` | 查询本人商品，编辑描述，执行上下架和删除 |
| API | `SecondhandProductController` | 校验请求并暴露 UC16 HTTP 接口 |
| 领域服务 | `SecondhandProductServiceImpl` | 校验分类、价格、所有权和状态，持久化商品 |
| 风险服务 | `ProductRiskAuditServiceImpl` | 创建或编辑后生成商品风险审核记录 |
| 数据访问 | `SecondhandProductMapper`、`CategoryMapper` | 访问 `secondhand_product` 和 `category` |

## 状态与约束

`secondhand_product.status` 使用三个互斥值：`1=在售`、`2=下架`、`3=已售`。卖家只可在在售和下架之间切换；直接购买、议价确认或拍卖成交后进入已售，已售商品不得编辑、重新上架或删除。

发布和编辑要求名称、1-9 张图片、原价、售价、一级/二级分类、成色和议价标志有效。售价不得高于原价；二级分类必须属于所选一级分类。所有写操作使用 JWT 中的当前用户校验商品归属。

## 验证入口

- Integration：`SecondhandProductManagementIntegrationTest`
- Browser E2E：`frontend/e2e/domain-d/uc16-product-management.spec.ts`
- 测试计划：[04_tests/UC16/test-plan.md](../../04_tests/UC16/test-plan.md)
- 测试报告：[04_tests/UC16/test-report.md](../../04_tests/UC16/test-report.md)
