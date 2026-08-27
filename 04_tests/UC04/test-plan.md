# UC04 test plan

```bash
mvn -B -f backend/pom.xml -Dtest=UserGovernanceUc04IntegrationTest test
cd frontend && npm run e2e -- e2e/domain-a/uc04-ban-unban.spec.ts
```

The gate covers ban/unban/login linkage, no-token and non-admin rejection,
self-ban protection, repeated unban semantics, and administrator-only audit
queries. Compose/MySQL results go under this UC's evidence directory.
