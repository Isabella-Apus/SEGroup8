# UC03 test plan

```bash
mvn -B -f backend/pom.xml -Dtest=MerchantApplicationUc03IntegrationTest,MerchantApplicationNotificationFailureIntegrationTest test
cd frontend && npm run e2e -- e2e/domain-a/uc03-merchant-application.spec.ts
```

Acceptance covers submission/query, admin review permission, approve/reject
state transitions, role/shop/audit consistency, repeat approve idempotency and
notification failure isolation. Compose/MySQL browser output belongs in this
UC's evidence directory.
