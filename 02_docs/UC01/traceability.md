# UC01 traceability

状态：后端集成与 API 证据已完成；浏览器 spec 已完成但真实 Compose 执行未完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ01 register with hashed password and default USER/NORMAL | `AuthServiceImpl.register` | `IdentityUc01IntegrationTest.registerLoginRoleBoundaryAndBanMustShareOnePersistedChain` | `uc01-auth.spec.ts` register/login | 后端已完成；浏览器待运行 |
| REQ01 valid JWT login | `AuthServiceImpl.login`, `JwtUtils` | same test, JWT claims assertion | browser login and reload | 后端已完成；浏览器待运行 |
| REQ01 USER cannot access admin API | `AdminUserServiceImpl` | same test | API-assisted browser test | 后端已完成；浏览器待运行 |
| REQ01 banned user cannot login | `AdminUserController`, `AuthServiceImpl` | same test | same spec | 后端已完成；浏览器待运行 |
| REQ01 duplicate/invalid/wrong password matrix | validation and `AuthServiceImpl` | `duplicateInvalidAndWrongPasswordRequestsMustNotCreateDirtyUsers` | API setup path | 已完成 |
