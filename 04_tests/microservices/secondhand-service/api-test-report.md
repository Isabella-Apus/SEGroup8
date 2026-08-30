# MS-04 API 与集成测试报告

最近本地执行时间：2026-08-30（Asia/Shanghai）

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl secondhand-service -am clean verify
```

## 结果

| 模块 | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `security-contract` | 5 | 0 | 0 | 0 |
| `secondhand-service` | 20 | 0 | 0 | 0 |
| 合计 | 25 | 0 | 0 | 0 |

## 公开接口完整性

- 运行时 OpenAPI 契约精确校验 `21/21` 个公开操作，接口增删会使测试失败。
- 21 个公开操作均有成功路径或完整业务路径 API 断言；16 个受保护操作另有匿名访问 `401` 矩阵。
- UC16 商品、UC17 直购、UC18 议价、UC19 拍卖以及 UC20 订单事件协作均有 API/契约测试。
- `/actuator` 链接中不再暴露 `flyway`，直接访问 `/actuator/flyway` 返回 `404`。
- 请求关联头 `X-Request-Id`、`X-Trace-Id` 有回传断言。

## 集成与边界

- Testcontainers MySQL 8.4.6 验证 Flyway 自有表、并发、恢复和幂等。
- `secondhand_app` 对自有表可写，对 `order_db.order_info` 的写入由数据库权限拒绝。
- 源码门禁拒绝订单、余额、优惠券、通知 Mapper 和跨域订单 SQL。

原始历史报告保留在 `evidence/raw-reports/surefire/`；本次最新报告由 Actions artifact 保存，避免将每次生成物长期提交到 Git。
