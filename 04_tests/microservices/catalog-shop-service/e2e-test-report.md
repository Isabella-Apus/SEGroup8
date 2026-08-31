# E2E 测试报告

测试源码唯一位置为 `frontend/e2e/domain-b/`：UC06 商品搜索详情、UC07 商品生命周期、UC08 店铺、UC09 风险审核、UC10 行为历史。

PR CI 有两道必需门禁：独立 `compose.acceptance.yml` 启动 catalog、MySQL、身份桩和真实前端，在浏览器执行 UC06；根 `compose.yml` 则由平台 CI 执行 UC06-UC10 完整浏览器回归。Playwright 结果、截图和 trace 由 CI 写入本目录的 `independent-e2e/` 或平台 E2E artifact；本机无 Docker 时不将其标注为通过。
