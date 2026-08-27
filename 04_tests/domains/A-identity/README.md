# Domain-A evidence index

Shared evidence and test-boundary documentation belongs here. Business
acceptance evidence belongs in `04_tests/UC01` through `04_tests/UC05` so each
Task can be reviewed independently.

The Compose runner writes logs and Playwright artifacts under the selected UC
evidence directory when `E2E_OUTPUT_DIR` is set. Platform smoke evidence
continues to live under `04_tests/platform-e2e/evidence`.

The shared JWT boundary is reported as `PLATFORM`, not duplicated into every
UC. The final Domain-A gate is:

```powershell
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
```
