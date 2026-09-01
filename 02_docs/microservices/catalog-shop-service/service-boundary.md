# 服务边界

唯一入口为 `com.segroup8.catalogshop.CatalogShopApplication`。进程内部的 catalog 模块负责分类、商品与库存；shop 模块负责店铺及 `MerchantApproved.v1`；risk 模块负责规则降级、人工决策和通知 outbox；behavior 模块负责用户浏览、搜索与热词。

商品和库存共享本地事务，订单只调用 `/internal/inventory/**`，不能写商品表。外部身份均为 `sellerId`、`userId`、`adminId` 投影值，本服务的数据源账号仅授权 `catalog_shop_db.*`，禁止查询身份库。

公开契约只使用 `/api/category/**`、`/api/product/**`、`/api/shop/**`、`/api/admin/product-risk-audits/**`、`/api/user/browse-history/**`、`/api/search/**`。旧路径只能在 Gateway 限时映射，并添加 `Deprecation: true`、`Sunset` 和 `Link` 响应头；服务本身不暴露旧路径。
