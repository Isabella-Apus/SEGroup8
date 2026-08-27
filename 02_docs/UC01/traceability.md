# UC01 traceability

2026-08-27 refresh: 后端集成与真实 Compose + MySQL + Chromium Playwright
执行均已完成；本次 UC01 spec 通过 1 个测试。对应 raw report、HTML/JSON
结果和 Compose 日志位于 `04_tests/UC01/evidence/`。

状态：后端集成与 API 证据已完成；真实 Compose + MySQL + Chromium 执行已完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ01 register with hashed password and default USER/NORMAL | `AuthServiceImpl.register` | `IdentityUc01IntegrationTest.registerLoginRoleBoundaryAndBanMustShareOnePersistedChain` | `uc01-auth.spec.ts` register/login | 后端与浏览器已完成 |
| REQ01 valid JWT login | `AuthServiceImpl.login`, `JwtUtils` | same test, JWT claims assertion | browser login and reload | 后端与浏览器已完成 |
| REQ01 USER cannot access admin API | `AdminUserServiceImpl` | same test | API-assisted browser test | 后端与浏览器已完成 |
| REQ01 banned user cannot login | `AdminUserController`, `AuthServiceImpl` | same test | same spec | 后端与浏览器已完成 |
| REQ01 duplicate/invalid/wrong password matrix | validation and `AuthServiceImpl` | `duplicateInvalidAndWrongPasswordRequestsMustNotCreateDirtyUsers` | API setup path | 已完成 |
