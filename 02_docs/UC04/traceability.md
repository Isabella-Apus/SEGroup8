# UC04 traceability

| Requirement / acceptance | Implementation | Integration | E2E |
|---|---|---|---|
| REQ04 admin ban/unban state | `AdminUserServiceImpl` | `UserGovernanceUc04IntegrationTest` | ban/unban API-assisted browser flow |
| REQ04 banned login failure and recovery | `AuthServiceImpl.login` | same test | login before/after recovery |
| REQ04 non-admin and self-ban boundaries | `assertAdmin`, self check | same test | API permission path |
| REQ04 audit linkage/query | controller audit record + `AdminAuditLogService` | audit count/query | admin audit API query |
