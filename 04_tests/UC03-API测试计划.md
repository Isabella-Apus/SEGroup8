# UC03 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT03-01 | `MerchantApplicationControllerWebMvcTest.submit_shouldReturnSuccess` | Controller API | 用户提交申请 |
| INT03-02 | `MerchantApplicationControllerWebMvcTest.getMyApplication_shouldReturnRecord` | Controller API | 本人申请查询 |
| INT03-03 | `MerchantApplicationControllerWebMvcTest.page_shouldReturnAdminApplicationQueue` | Controller API | 管理员申请队列 |
| INT03-04 | `MerchantApplicationControllerWebMvcTest.reject_shouldRequireReason` | Controller API | 拒绝原因参数校验 |
| INT03-05 | `MerchantApplicationControllerWebMvcTest.reject_shouldReturnSuccessAndRecordAudit` | Controller API | 拒绝成功和审计调用 |
| INT03-06 | `MerchantApplicationControllerWebMvcTest.approve_shouldReturnSuccessAndRecordAudit` | Controller API | 管理员通过和审计调用 |
| UNIT03-01 | `MerchantApplicationServiceImplTest.submit_shouldRejectDuplicatePendingApplication` | Service Unit | 重复申请异常 |
| UNIT03-02 | `MerchantApplicationServiceImplTest.reject_shouldPersistReason` | Service Unit | 拒绝状态和原因 |
| UNIT03-03 | `MerchantApplicationServiceImplTest.approve_shouldUpgradeRoleAndInsertNotification` | Service Unit | 角色升级和通知 |

定向命令：

```powershell
mvn -Dtest=MerchantApplicationControllerWebMvcTest,MerchantApplicationServiceImplTest test
```

## API 断言规则

- 申请提交/审核成功都断言 `code=0`、`message=success`。
- 拒绝接口缺失 `rejectReason` 时断言 `code=400`，Service 不被调用。
- 审核通过/拒绝必须验证 Controller 调用了审计服务；角色/店铺/通知的细节由 Service 测试验证。
- 当前 API 测试验证路由、参数和委托；完整管理员 JWT、真实事务回滚和前端 E2E 留到后续回归。
