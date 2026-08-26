# UC04 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT04-01 | `AdminUserControllerWebMvcTest.ban_shouldReturnSuccessAndRecordAudit` | Controller API | 封禁路由、统一响应、审计调用 |
| INT04-02 | `AdminUserControllerWebMvcTest.unban_shouldReturnSuccessAndRecordAudit` | Controller API | 解禁路由、统一响应、审计调用 |
| INT04-03 | `AdminUserControllerWebMvcTest.pageUsers_shouldReturnAdminUserPage` | Controller API | 管理员用户列表 |
| INT04-04 | `AdminUserControllerWebMvcTest.pageAuditLogs_shouldReturnAdminAuditPage` | Controller API | 审计日志查询 |
| UNIT04-01 | `AdminUserServiceImplTest.banUser_shouldUpdateStatusToBanned` | Service Unit | 封禁状态 |
| UNIT04-02 | `AdminUserServiceImplTest.unbanUser_shouldRestoreNormalStatus` | Service Unit | 解禁状态 |
| UNIT04-03 | `AdminUserServiceImplTest.banUser_shouldRejectSelfBan` | Service Unit | 管理员自我保护 |
| UNIT04-04 | `AdminUserServiceImplTest.banUser_shouldThrowWhenCurrentUserNotAdmin` | Service Unit | 非管理员拒绝 |
| UNIT04-05 | `AdminAuditLogServiceImplTest.record_shouldInsertAuditLog` | Service Unit | 审计写入 |

定向命令：

```powershell
mvn -Dtest=AdminUserControllerWebMvcTest,AdminUserServiceImplTest,AdminAuditLogServiceImplTest test
```

## API 断言规则

- 封禁和解禁成功均断言 `code=0`，并验证 `AdminAuditLogService.record` 被调用。
- Service 测试断言目标用户状态，不只判断方法未抛异常。
- Controller API 已覆盖用户列表、封禁、解禁和审计查询的路由契约；管理员身份由 Service/权限测试验证。
- 封禁后登录、旧 Token 失效策略、真实数据库审计查询和完整 HTTP 链列入 Day 4/回归任务。
