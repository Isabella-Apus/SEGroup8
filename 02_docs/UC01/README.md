# UC01 注册、登录与角色联动

状态：后端/API/H2 集成与 E2E 脚本已完成；真实 Compose/MySQL 浏览器执行未完成。

UC01 的可审计入口。业务实现沿用现有 Auth/AdminUser 服务；本分支补充
Spring Boot + H2 的 HTTP 集成链和 Compose 前端 Playwright 入口。

- 需求：`REQ01 / UC01`
- 集成测试：`IdentityUc01IntegrationTest`
- 浏览器测试：`frontend/e2e/domain-a/uc01-auth.spec.ts`
- 运行证据：`04_tests/UC01/evidence/`

这是成员 A 的独立分支交付；UC 父 Issue 仍需等待后续微服务阶段完成。
