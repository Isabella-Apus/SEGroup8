# UC02 API 测试计划

## 测试类与命令

| 测试编号 | 测试类/方法 | 类型 | 覆盖内容 |
|---|---|---|---|
| INT02-01 | `UserControllerWebMvcTest.profile_shouldReturnCurrentUser` | Controller API | 资料查询和统一响应 |
| INT02-02 | `UserControllerWebMvcTest.me_shouldReturnCurrentUser` | Controller API | `/me` 兼容查询 |
| INT02-03 | `UserControllerWebMvcTest.updateProfile_shouldReturnSuccess` | Controller API | 资料修改 |
| INT02-04 | `UserControllerWebMvcTest.createAddress_shouldReturnSuccess` | Controller API | 地址新增 |
| INT02-05 | `UserControllerWebMvcTest.createAddress_shouldRejectInvalidPhone` | Controller API | 地址参数校验 |
| INT02-06 | `UserControllerWebMvcTest.listAddresses_shouldReturnOnlyServiceResult` | Controller API | 地址查询 |
| INT02-07 | `UserControllerWebMvcTest.updateAddress_shouldReturnSuccess` | Controller API | 地址修改 |
| INT02-08 | `UserControllerWebMvcTest.deleteAddress_shouldReturnSuccess` | Controller API | 地址删除和 Service 调用 |
| UNIT02-01 | `UserServiceImplTest.getCurrentUserProfile_shouldMapUserInfo` | Service Unit | 资料映射 |
| UNIT02-02 | `UserServiceImplTest.createAddress_whenDefault_shouldClearPreviousDefault` | Service Unit | 默认地址唯一 |
| UNIT02-03 | `UserServiceImplTest.deleteAddress_shouldThrowWhenAddressNotOwned` | Service Unit | 地址归属异常 |

定向命令：

```powershell
mvn -f backend/pom.xml -Dtest=UserControllerWebMvcTest,UserServiceImplTest test
```

## API 断言规则

- 成功响应断言 `code=0`、`message=success` 和关键资料/地址字段。
- 非法手机号在 Controller 层被拦截，Service 不应被调用。
- 地址归属和默认地址唯一性由 Service 层测试验证，Controller 层负责请求和委托契约。
- 当前 API 测试使用 standalone MockMvc，不替代多用户真实数据库隔离；该部分和前端 E2E 在 Day 4 处理。
