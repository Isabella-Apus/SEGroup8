# 测试计划

| 层级 | 范围 | 通过标准 |
|---|---|---|
| unit/API | UC06-UC10、校验、所有权、无 LLM key | Maven tests 0 failure |
| migration | H2 空 Schema；MySQL 8 clean/legacy import；跨库拒绝 | Flyway 成功，拒绝身份库 |
| concurrency/contract | 多商品原子预留、幂等 confirm/release、过期 | 不超卖，重复结果一致 |
| E2E | 复用 `frontend/e2e/domain-b/uc06` 至 `uc10` | 5 个 spec 全通过 |
| deployment/HPA | Helm atomic、探针、版本、回滚、搜索负载 | 无错误发布；指标和证据齐全 |

本地已执行 Maven API/迁移测试。MySQL Compose、浏览器、K3s 与 HPA 必须在对应环境执行并保存原始证据。
