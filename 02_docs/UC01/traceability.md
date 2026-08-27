# UC01 traceability

| Requirement / acceptance | Implementation | Integration | E2E |
|---|---|---|---|
| REQ01 register with hashed password and default USER/NORMAL | `AuthServiceImpl.register` | `IdentityUc01IntegrationTest.registerLoginRoleBoundaryAndBanMustShareOnePersistedChain` | `uc01-auth.spec.ts` register/login |
| REQ01 valid JWT login | `AuthServiceImpl.login`, `JwtUtils` | same test, JWT claims assertion | browser login and reload |
| REQ01 USER cannot access admin API | `AdminUserServiceImpl` | same test | API-assisted browser test |
| REQ01 banned user cannot login | `AdminUserController`, `AuthServiceImpl` | same test | same spec |
| REQ01 duplicate/invalid/wrong password matrix | validation and `AuthServiceImpl` | `duplicateInvalidAndWrongPasswordRequestsMustNotCreateDirtyUsers` | API setup path |
