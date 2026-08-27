# UC01 test plan

## Scope

Register, login, password hashing, JWT claims, role authorization, ban/login
linkage, duplicate registration, invalid parameters and wrong passwords.

## Commands

```bash
mvn -B -f backend/pom.xml -Dtest=IdentityUc01IntegrationTest test
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
cd frontend && npm run build:real
```

The browser gate is full-stack Compose + MySQL and is run with
`E2E_OUTPUT_DIR=04_tests/UC01/evidence`.
