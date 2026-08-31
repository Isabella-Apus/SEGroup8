# 服务边界

## Owner 事实

`identity-governance-service` 是 `user`、`address`、`merchant_application`、`user_report`、`report`（只读归档）、`user_block`、`credit_score_log`、`admin_audit_log` 以及本 Schema 技术表的唯一 owner。其他服务不得引用本模块的 JDBC/Repository，也不得连接 `identity_governance_db`。

## UC 边界

| UC | 服务内职责 | 对外结果 |
|---|---|---|
| UC01 | 注册、密码哈希、登录、短期 JWT | `userId`、角色和 Bearer JWT |
| UC02 | 资料、用户摘要、地址所有权与唯一默认地址 | 资料/地址 API；订单侧复制地址快照 |
| UC03 | 商家申请和审核 | `MerchantApproved.v1` outbox 事件 |
| UC04 | 管理员用户查询、封禁/解禁、审计 | `UserAccessChanged.v1` outbox 事件 |
| UC05 | 举报、拉黑、信用调整与治理审计 | block-check 内部 API、信用 API |

登录是业务前置条件，但不是其他服务每次请求的同步依赖。其他服务使用 `security-contract` 本地验签；只有权限缓存缺失的高风险操作才调用 introspection。

## 禁止项

- 禁止跨 Schema JOIN、共享 Mapper、共享写表和客户端伪造用户 ID 请求头。
- 禁止审批与远端建店双写；本地审批和 outbox 必须同事务。
- 禁止在日志中记录密码、JWT、数据库口令或内部服务 Token。
