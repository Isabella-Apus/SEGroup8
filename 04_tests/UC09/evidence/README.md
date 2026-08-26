# UC09 可复核测试证据

重新运行：`mvn -B -f microservices/risk-service/pom.xml clean test`

- `logs/uc09-maven-clean-test.log`：UC09 完整原始输出。
- `logs/microservices-regression-test.log`：UC06 + UC09 回归输出。
- `raw-reports/`：Maven Surefire 机器可读 XML 与文本报告。
- `result-summary.json`：结构化结果摘要。
- `index.html`、`screenshots/`：便于评审的可视化证据。
