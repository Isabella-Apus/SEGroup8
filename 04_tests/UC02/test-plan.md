# UC02 test plan

```bash
mvn -B -f backend/pom.xml -Dtest=ProfileAddressUc02IntegrationTest test
cd frontend && npm run e2e -- e2e/domain-a/uc02-profile-address.spec.ts
```

Acceptance includes profile consistency, address CRUD, default-address
uniqueness, deleted-row invisibility, cross-user update/delete rejection and
missing-token rejection. Browser execution requires Compose frontend/backend/
MySQL and uses `E2E_OUTPUT_DIR=04_tests/UC02/evidence`.
