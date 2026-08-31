# V3 final regression evidence

Date: 2026-08-31. Candidate revision: `684aff9e87664ebb41d9844cacbfd8bdf2dc60b3`. Output is intentionally summarized to exclude credentials,
JWTs, event payloads, notification text, and message bodies.

| Command | Result | Observed |
|---|---|---|
| `mvn -B -f microservices/pom.xml -pl messaging-service -am clean verify` | PASS | Messaging and security-contract reactor completed; 22 Messaging tests passed, 1 conditional MySQL test skipped |
| `mvn -B -f microservices/pom.xml clean test` | PASS | Full microservices reactor completed; Messaging 22/0/0 with 1 conditional skip |
| `mvn -B -f backend/pom.xml test` | PASS | 235 tests, 0 failures, 0 errors, 0 skipped |
| `npm --prefix frontend run build` | PASS | Vite production bundle built; existing chunk-size warnings only |
| `git diff --check` | PASS | No whitespace errors |

The backend log contained a shutdown-only scheduler connection warning while a
Testcontainers MySQL instance was being torn down; Maven still reported BUILD
SUCCESS and no test failure.
