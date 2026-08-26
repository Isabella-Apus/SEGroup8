# UC01 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT01-01 | `AuthControllerWebMvcTest.register_shouldReturnUnifiedSuccess` | Controller API | 注册成功、`code/message/data` |
| INT01-02 | `AuthControllerWebMvcTest.login_shouldReturnTokenAndRole` | Controller API | 登录返回 JWT、角色、用户摘要 |
| INT01-03 | `AuthControllerWebMvcTest.register_shouldRejectInvalidBody` | Controller API | 必填字段和格式校验 |
| INT01-04 | `JwtUtilsTest.parse_shouldRejectTamperedToken` | JWT 单元 | 签名篡改拒绝 |
| UNIT01-01 | `AuthServiceImplTest.register_shouldEncodePasswordAndInsertUser` | Service Unit | 密码加密和用户初始化 |
| UNIT01-02 | `AuthServiceImplTest.login_shouldUpgradeLegacyPasswordAndReturnToken` | Service Unit | 旧密码升级和 JWT 委托 |

定向命令：

```powershell
mvn -f backend/pom.xml -Dtest=AuthControllerWebMvcTest,JwtUtilsTest,AuthServiceImplTest test
```

## API 断言规则

- 成功接口同时断言 HTTP 200、`$.code=0`、`$.message=success` 和关键 `data` 字段。
- 参数失败断言 `$.code=400`，并验证 Service 未被调用。
- JWT 测试不使用固定线上密钥，不提交真实 Token。
- 完整 Spring 拦截器、数据库和前端 E2E 留在后续回归任务。

