# UC10 可复核测试证据

重新运行：`mvn -B -f microservices/behavior-service/pom.xml clean test`

- `logs/uc10-maven-clean-test.log`：UC10 完整原始输出。
- `logs/microservices-regression-test.log`：UC06 + UC10 回归输出。
- `raw-reports/`：Surefire XML 与文本报告。
- `result-summary.json`：结构化摘要。
- `index.html`、`screenshots/`：可视化证据。
