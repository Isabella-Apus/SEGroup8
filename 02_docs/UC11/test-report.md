# UC11 测试报告

## 执行状态

报告由当前 main实际验证结果更新。不得以测试计划、Mock 声明或其他 UC 测试替代 UC11 结果。

| 验证项 | 命令/入口 | 当前结果 | 原始证据 |
|---|---|---|---|
| UC11 Domain-C/MySQL | Order 领域测试与真实 MySQL | PASS | Order 流水线 33526387441 |
| UC11 浏览器 E2E | `uc11-checkout-order.spec.ts` | PASS | 完整系统流水线 33526387696 与 Actions artifact |
| 后端、前端和完整回归 | Maven、Vite、UC01-UC25 | PASS | 完整系统流水线 33526387696 |

## 场景结论

真实 MySQL 8.4.6 Integration 已覆盖创建主链、重复项合并与服务端计价、商品和交易资格拒绝、优惠券边界、券占用后的后续明细写入失败事务回滚、幂等回放及 HTTP/数据库一致性。浏览器 E2E 已在 MySQL 8.4.6、真实后端 JAR 和 real-mode 前端上完成购物车到订单详情链路，并在刷新后重新查询确认持久化。

早期 Docker 镜像代理 EOF 属于已经解决的历史过程，不再作为当前交付状态。最终结果以结构化结果和当前 Actions 执行为准。
