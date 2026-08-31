# E2E 测试报告

状态：本次未运行。现有 UC11-UC15 与 UC20 Playwright spec 保持原位置并复用，没有复制到服务目录。开发代理与 Helm Ingress 已把 `/api/order`、`/api/review`、`/api/logistics`、`/api/admin/orders` 路由到 order-service；仍需在 catalog-shop、benefits-finance、messaging、secondhand 调用方全部部署后运行并回填浏览器版本、commit、6 条用例结果和截图路径。
