# UC12 evidence

Generated backend and browser evidence belongs here:

- `logs/backend-domain-c.log` - Domain-C launcher output.
- `raw-reports/surefire/` - Surefire XML and text reports.
- `screenshots/` - browser screenshots after refresh/requery.
- `result-summary.json` - machine-readable backend totals and result.

Generate backend evidence with:

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC12 --goal verify --maven-repository backend/.m2repo
```
