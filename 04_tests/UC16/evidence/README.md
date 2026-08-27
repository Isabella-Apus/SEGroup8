# UC16 Evidence

本目录只保存实际执行产物，不以人工截图替代自动化结果。

| 目录 | 内容 |
|---|---|
| `raw-reports/surefire/` | H2 Integration 的 Surefire XML/TXT |
| `raw-reports/mysql/` | 同一测试类连接 Compose MySQL 8 的 Surefire XML/TXT |
| `raw-reports/playwright/` | Playwright JSON、JUnit、HTML report、trace、video 和附件 |
| `logs/` | Compose 构建、启动、健康检查、三容器日志与 Playwright 命令日志 |
| `screenshots/` | 用例主动保存的关键业务状态截图 |

`result-summary.json` 汇总三套执行结果。Playwright runner 使用空数据库卷，访问真实 Nginx 前端，并由前端经真实 Spring Boot API 读取和写入 MySQL seed 数据。
