# 改造前后版本与一致性报告

| 项目 | 改造前 | 改造后 |
|---|---|---|
| 基线 | `main` (`bb72290cff96c78ab189468b82db1f8ba3cd9323`) | tested revision `cbc1ab300d0bf3c78669d1c0ac9f72fd5a390e3a`；merge SHA 待合并后填写 |
| 券逻辑 | 单体 `VoucherService` | 独立 `VoucherService` + `user_voucher` CAS 状态机 |
| 资金逻辑 | 单体 `EscrowSettlementService` | 独立事务 worker + 请求/流水/Outbox |
| 调用方式 | 订单直接注入 Mapper/Service | 内部 HTTP 契约与请求 ID 查询 |
| 数据库 | 单体 Schema | `benefits_finance_db` 独占 |

同样的初始个人余额 100.00、扣款 60.00、退款 20.00 后均得到 60.00；商家结算 40.00 后经营余额为 40.00。自动化测试同时断言流水数量、Outbox 数量及重复请求不重复入账。原始 Surefire 报告位于测试证据目录。

2026-08-31 本机验证已通过 Testcontainers 1.21.4 实际启动 MySQL 8.4.6：Flyway migrator 与运行 DML 用户不同，运行账号不能 DDL 或写入 `order_db`，相关 MySQL 契约 4/4 PASS、0 跳过；完整服务测试为 26/26 PASS，复用的安全契约为 5/5 PASS。候选镜像的独立服务 API E2E 为 3/3 PASS；旧 Domain E 平台 Compose 兼容回归为 3/3 PASS，但因当前栈未接入 order/messaging/finance 路由，真实 finance 路由 E2E 为 `NOT_RUN`。

镜像 digest、Helm revision 和最终 merge SHA 必须由 CI/发布环境生成，当前保持 `PENDING`，不得用本地占位值冒充发布证据。
