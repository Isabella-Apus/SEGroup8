# UC10 浏览记录、搜索历史和热词测试报告

## API 集成测试

- 命令：`mvn -B --no-transfer-progress -f microservices/pom.xml -Pdomain-b clean test`
- 环境：Java 17、Spring Boot 3.3.4、H2、MockMvc
- 对应套件：`BehaviorApiIntegrationTest`（带 `DOMAIN_B` 标签）

该套件验证搜索词规范化、空值拒绝、浏览记录去重与用户隔离、删除边界、热词计数排序及 Top 限制。它是 API 集成测试，不替代真实浏览器 E2E。

## 真实浏览器 E2E

- 命令：`powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser`
- 环境：Docker Compose + MySQL + 前端真实构建 + Playwright
- 对应套件：`frontend/e2e/domain-b/uc10-behavior.spec.ts`

浏览器场景覆盖用户浏览商品和搜索后查看搜索历史/热词、删除浏览记录并刷新确认持久化，以及不同用户之间的浏览记录隔离和匿名访问被拒绝。

GitHub Actions 的 `domain-b-browser-e2e` 作业会上传 `domain-b-playwright-reports` artifact，其中包含 Playwright JUnit/JSON/HTML 报告、失败追踪资料和执行日志；不要将这些生成物提交到仓库。
