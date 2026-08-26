# UC01 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT01-01 | `AuthControllerWebMvcTest.register_shouldReturnUnifiedSuccess` | Controller API | 注册成功、`code/message/data` |
| INT01-02 | `AuthControllerWebMvcTest.login_shouldReturnTokenAndRole` | Controller API | 登录返回 JWT、角色、用户摘要 |
| INT01-03 | `AuthControllerWebMvcTest.register_shouldRejectInvalidBody` | Controller API | 必填字段和格式校验 |
| INT01-04 | `AuthControllerWebMvcTest.login_shouldRejectInvalidBody` | Controller API | 登录参数校验 |
| INT01-05 | `JwtAuthInterceptorTest.preHandle_shouldRejectMissingBearerHeader` | JWT 拦截器 | 缺失 Bearer Token 返回 401 |
| INT01-06 | `JwtAuthInterceptorTest.preHandle_shouldParseTokenAndPopulateUserContext` | JWT 拦截器 | 解析 uid 并写入用户上下文 |
| INT01-07 | `JwtUtilsTest.parse_shouldRejectTamperedToken` | JWT 单元 | 单体签名篡改拒绝 |
| UNIT01-01 | `AuthServiceImplTest.register_shouldEncodePasswordAndInsertUser` | Service Unit | 密码加密和用户初始化 |
| UNIT01-02 | `AuthServiceImplTest.login_shouldUpgradeLegacyPasswordAndReturnToken` | Service Unit | 旧密码升级和 JWT 委托 |
| UNIT01-03 | `AuthServiceImplTest.register_shouldRejectDuplicateUsername` | Service Unit | 重复用户名拒绝 |
| UNIT01-04 | `AuthServiceImplTest.login_shouldRejectBannedUser` | Service Unit | 封禁账号不得签发 Token |
| SEC01-01~05 | `microservices/security-contract` 的 `JwtTokenVerifierTest` | 跨微服务安全契约 | claims 兼容、篡改、过期、Bearer 格式和弱 secret |

定向命令：

```powershell
mvn -f backend/pom.xml -Dtest=AuthControllerWebMvcTest,JwtAuthInterceptorTest,JwtUtilsTest,AuthServiceImplTest test
```

微服务 JWT 契约测试：

```powershell
mvn -f microservices/security-contract/pom.xml test
```

## API 断言规则

- 成功接口同时断言 HTTP 200、`$.code=0`、`$.message=success` 和关键 `data` 字段。
- 参数失败断言 `$.code=400`，并验证 Service 未被调用。
- JWT 测试不使用固定线上密钥，不提交真实 Token。
- Controller 测试验证接口契约；拦截器和微服务安全契约分别由 `JwtAuthInterceptorTest`、`JwtTokenVerifierTest` 验证。
- 完整真实数据库、多角色跨接口链和前端 E2E 留在后续回归任务。
