# UC02 traceability

| Requirement / acceptance | Implementation | Integration | E2E |
|---|---|---|---|
| REQ02 profile update and re-query | `UserServiceImpl.updateCurrentUserProfile` | profile assertions in `ProfileAddressUc02IntegrationTest` | profile reload |
| REQ02 address create/update/delete | `UserServiceImpl` address methods | same test | address page reload |
| REQ02 max one default per owner | `clearDefaultAddress` | database count assertion | response assertion |
| REQ02 cross-user isolation and unauthenticated rejection | `getOwnedAddress`, JWT interceptor | ownership test | API-assisted E2E |
