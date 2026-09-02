# 服务接口清单与 API 测试映射

运行时 Springdoc 与 `openapi.yaml` 一致：共 35 个 method-path，其中公开接口 31 个、内部接口 4 个。下面每个公开接口都有成功路径断言；除注册/登录自身的输入与凭据异常测试外，其余 29 个接口统一经过匿名拒绝、失效账户 JWT 拒绝，管理员接口还经过普通用户越权拒绝。

| 方法 | 路径 | UC | 成功路径测试 |
|---|---|---|---|
| POST | `/api/auth/register` | UC01 | `AuthenticationApiTest` |
| POST | `/api/auth/login` | UC01 | `AuthenticationApiTest` |
| GET | `/api/user/profile` | UC02 | `PublicApiSuccessCoverageTest` |
| PUT | `/api/user/profile` | UC02 | `PublicApiSuccessCoverageTest` |
| GET | `/api/user/me` | UC02 | `PublicApiSuccessCoverageTest` |
| GET | `/api/user/search` | UC02/UC24 | `PublicApiSuccessCoverageTest` |
| GET | `/api/user/addresses` | UC02 | `PublicApiSuccessCoverageTest` |
| POST | `/api/user/addresses` | UC02 | `PublicApiSuccessCoverageTest` |
| PUT | `/api/user/addresses/{addressId}` | UC02 | `PublicApiSuccessCoverageTest` |
| DELETE | `/api/user/addresses/{addressId}` | UC02 | `PublicApiSuccessCoverageTest` |
| POST | `/api/user/merchant-application` | UC03 | `PublicApiSuccessCoverageTest` |
| GET | `/api/user/merchant-application/me` | UC03 | `PublicApiSuccessCoverageTest` |
| GET | `/api/admin/merchant-applications` | UC03 | `PublicApiSuccessCoverageTest` |
| POST | `/api/admin/merchant-applications/{applicationId}/approve` | UC03 | `PublicApiSuccessCoverageTest` |
| POST | `/api/admin/merchant-applications/{applicationId}/reject` | UC03 | `PublicApiSuccessCoverageTest` |
| GET | `/api/admin/users` | UC04 | `PublicApiSuccessCoverageTest` |
| PUT | `/api/admin/users/{userId}/ban` | UC04 | `PublicApiSuccessCoverageTest` |
| PUT | `/api/admin/users/{userId}/unban` | UC04 | `PublicApiSuccessCoverageTest` |
| GET | `/api/admin/audit-logs` | UC04/UC05 | `PublicApiSuccessCoverageTest` |
| POST | `/api/report-block/report` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/report-block/report/my` | UC05 | `PublicApiSuccessCoverageTest` |
| POST | `/api/report-block/block` | UC05 | `PublicApiSuccessCoverageTest` |
| DELETE | `/api/report-block/block/{targetUserId}` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/report-block/block/my` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/report-block/block/check/{targetUserId}` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/report-block/block/blocked-by/{targetUserId}` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/credit/me` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/credit/{userId}` | UC05 | `PublicApiSuccessCoverageTest` |
| GET | `/api/admin/reports` | UC05 | `PublicApiSuccessCoverageTest` |
| POST | `/api/admin/reports/audit` | UC05 | `PublicApiSuccessCoverageTest` |
| POST | `/api/admin/reports/credit-adjust` | UC05 | `PublicApiSuccessCoverageTest` |

负向矩阵：

- `AuthenticationApiTest`：重复注册、错误密码、匿名访问和普通用户越权。
- `PublicApiSecurityContractTest#everyProtectedPublicPathRejectsAnonymousRequests`：29/29 受保护公开接口匿名拒绝。
- `PublicApiSecurityContractTest#everyAdminPathRejectsOrdinaryUsers`：10/10 管理接口普通用户越权拒绝。
- `PublicApiSecurityContractTest#everyProtectedPublicPathRejectsADeletedAccountToken`：29/29 受保护接口拒绝已删除账户的旧 JWT。
- `IdentityGovernanceFlowIntegrationTest`：地址所有权、重复审核、重复拉黑/审核等非法业务状态。

内部接口：

| 方法 | 路径 | 调用方 | 约束测试 |
|---|---|---|---|
| POST | `/internal/auth/introspect` | Gateway/高风险服务 | 服务 Token、Request ID、幂等键 |
| GET | `/internal/users/{userId}/summary` | 业务服务 | 服务 Token、Request ID、最小字段 |
| GET | `/internal/users/{userId}/address-snapshot?addressId={id}` | 已发布的 secondhand 消费者 | 服务 Token、Request ID、地址所有权；省略 addressId 时返回默认/首个地址 |
| GET | `/internal/users/{userId}/addresses/{addressId}` | 二手服务 | 服务 Token、Request ID、校验用户与地址归属、返回不可变建单快照 |
| GET | `/internal/users/{userId}/shipping-address` | 二手服务 | 服务 Token、Request ID、默认地址优先的不可变建单快照 |
| POST | `/internal/blocks/check` | messaging | 服务 Token、Request ID、幂等键、批量输入 |

机器可读契约仍以 `openapi.yaml` 为准；本文件用于课程检查和人工验收。
