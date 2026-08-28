# Domain D backend test report

## Classification

| Evidence | Current classification | Boundary |
|---|---|---|
| Tagged backend suite | `API_PASS` when all nine expected Surefire suites pass | Unit, standalone MockMvc contract, and Spring Boot H2 integration |
| Browser screenshots | `UI_WALKTHROUGH_PASS` | Vite mock only |
| Full-stack browser flow | `E2E_PENDING` | Five UC-specific Compose Playwright specs remain in Tasks #148-#152 |

## Reproducible Command

```powershell
cd backend
mvn -B --no-transfer-progress "-Dgroups=DOMAIN_D" clean verify
```

The authoritative class list and totals are stored in `evidence/result-summary.json`; raw XML is in `evidence/raw-reports/`, and the Maven console output is in `evidence/logs/domain-d-maven-test.log`.

## Expected Test Entry Points

1. `SecondhandProductServiceImplTest`
2. `SecondhandTradeServiceImplTest`
3. `SecondhandOrderFlowIntegrationTest`
4. `SecondhandAuctionIntegrationTest`
5. `SecondhandProductControllerUc16WebMvcTest`
6. `SecondhandProductControllerUc17WebMvcTest`
7. `SecondhandTradeControllerUc18WebMvcTest`
8. `SecondhandTradeControllerUc19WebMvcTest`
9. `OrderControllerUc20WebMvcTest`

The structured summary is generated from XML after execution; expected counts are never used as a substitute for actual results.

## Shared Platform Smoke

On 2026-08-27, the merged shared scaffold was verified with:

```powershell
.\scripts\e2e\run-compose-e2e.ps1 e2e/smoke/full-stack.smoke.spec.ts -ResetDatabase
```

Result: `1 passed`. The smoke used the Compose Nginx frontend, Spring Boot backend, and MySQL seed data. It proves the shared runner is usable; it does not change the five UC-specific Domain-D flows from `E2E_PENDING`.
