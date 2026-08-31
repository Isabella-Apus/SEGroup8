# 跨服务调用、事件与恢复

| 触发 | 契约 | 本地原子性 | 远端失败处理 |
|---|---|---|---|
| 商家审核通过 | `MerchantApproved.v1` | 申请状态、角色和 outbox 同事务 | catalog-shop 按 `eventId` 幂等；重试/DLQ；身份服务不写店铺表 |
| 封禁/解禁 | `UserAccessChanged.v1` | 用户状态、`access_version` 和 outbox 同事务 | 消费者幂等更新权限缓存；高风险写在版本未知时失败关闭 |
| 通知 | 后续由 messaging 消费治理事件 | 治理结果先提交 | 通知失败不回滚治理结果，保留 outbox 重试 |
| 用户摘要 | `GET /internal/users/{id}/summary` | 只读最小投影 | 调用者优先本地投影；失败使用旧快照，禁止跨库查询 |
| 地址快照 | `GET /internal/users/{userId}/addresses/{addressId}` | 校验地址属于买家后返回收件人、电话、省市和详细地址 | 二手服务创建订单前调用并冻结快照；不可用时拒绝建单，不使用占位地址 |
| 拉黑校验 | `POST /internal/blocks/check` | 本地批量只读 | messaging 无缓存且调用失败时拒绝建会话 |

`OutboxPublisher` 将 `MerchantApproved.v1` 先幂等投递到 catalog-shop 的
`POST /internal/events/merchant-approved`，再以标准 `EventEnvelope` 投递到 messaging；
`UserAccessChanged.v1` 直接投递到 messaging。只有所有目标成功后才标记 `PUBLISHED`，
失败采用有界指数退避，达到上限进入 `DEAD` 并保留 `last_error`，不会回滚已经提交的治理事实。

内部 API 必须同时携带 `X-Internal-Service-Token` 与 `X-Request-Id`；POST 还必须携带 `X-Idempotency-Key`。任何客户端传入的 `X-User-Id` 都不会建立用户上下文。
