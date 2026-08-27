# Domain-C Evidence

该目录由 `run-domain-c-tests.mjs` 和平台共享 E2E 启动器生成。提交前必须包含真实运行产生的 Maven 日志、同一 `reportSuffix` 批次的 Surefire XML/TXT、`result-summary.json`、Playwright JSON/HTML、Compose 日志和成功截图。

不得手工把 `result-summary.json` 改成 `PASS`。CI 会在运行前清理浏览器 Evidence，并分别上传后端和浏览器原始报告，避免已提交的旧文件参与当次验收。
