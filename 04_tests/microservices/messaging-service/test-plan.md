# MS-06 V3 test plan

| Area | Evidence command or artifact | Required outcome |
|---|---|---|
| Build | Maven reactor verify | messaging and security tests pass |
| Backend regression | `mvn -B -f backend/pom.xml test` | no code failures; Docker errors isolated |
| Runtime | live health/info/metrics probes | liveness/readiness/info and five metrics |
| Failure isolation | `evidence/raw-reports/scenario-c-live.md` | stop/restart preserves event and creates one notification |
| Container | Docker build with SHA tag | image builds without secrets |
| Helm | `helm lint` and `helm template` | chart renders with messaging values |
| Drill | deployment-failure-drill.md | failed readiness blocks and rollback recovers |
| Source boundary | `node scripts/ci/verify-messaging-boundary.mjs` | no foreign mapper/entity/schema access |
