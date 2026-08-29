# UC05 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT05-01 | `ReportBlockControllerWebMvcTest.submitReport_shouldReturnSuccess` | Controller API | 举报提交和统一响应 |
| INT05-02 | `ReportBlockControllerWebMvcTest.block_shouldRejectMissingTarget` | Controller API | 拉黑参数校验 |
| INT05-03 | `ReportBlockControllerWebMvcTest.myCredit_shouldReturnSuccess` | Controller API | 信用查询 |
| INT05-04 | `ReportBlockControllerWebMvcTest.auditReport_shouldRequireAdmin` | Controller API | 管理员审核、权限和审计调用 |
| UNIT05-01 | `ReportBlockServiceImplTest.submitReport_shouldRejectSelfReport` | Service Unit | 自举报异常 |
| UNIT05-02 | `ReportBlockServiceImplTest.submitReport_shouldInsertPendingReport` | Service Unit | 举报记录与状态 |
| UNIT05-03 | `ReportBlockServiceImplTest.blockUser_shouldRejectSelfBlock` | Service Unit | 自拉黑异常 |
| UNIT05-04 | `CreditServiceImplTest.adminAdjust_shouldRejectUnsupportedRole` | Service Unit | 非法信用维度 |
| UNIT05-05 | `CreditServiceImplTest.adminAdjust_shouldWriteScoreLog` | Service Unit | 用户积分和日志 |

定向命令：

```powershell
mvn -Dtest=ReportBlockControllerWebMvcTest,ReportBlockServiceImplTest,CreditServiceImplTest test
```

## API 断言规则

- 成功响应断言 `code=0`、`message=success` 和关键数据。
- 管理员审核必须在 Controller 测试中设置管理员上下文，并验证 `AccessControl` 通过及审计服务调用。
- Service 测试必须断言自操作、非法角色和积分上下限异常，不只判断“不报错”。
- 重复审核数据库链、跨角色完整治理链和 E2E 留到后续回归。

