# Domain-D Surefire summary

- API: **API_PASS**
- UI walkthrough: **UI_WALKTHROUGH_PASS** (Vite mock evidence only)
- Real Compose Playwright E2E: **E2E_PENDING**
- Classes: 9/9
- Tests: 20; Passed: 20; Failed: 0; Errors: 0; Skipped: 0

| Test class | Layer | UC | Tests | Passed | Failed | Errors | Skipped | Status |
|---|---|---|---:|---:|---:|---:|---:|---|
| `com.segroup8.platform.service.impl.SecondhandProductServiceImplTest` | service-unit | UC16, UC17 | 6 | 6 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.service.impl.SecondhandTradeServiceImplTest` | service-unit | UC18, UC19 | 6 | 6 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.integration.SecondhandOrderFlowIntegrationTest` | spring-h2-integration | UC20 | 1 | 1 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.integration.SecondhandAuctionIntegrationTest` | spring-h2-integration | UC19 | 1 | 1 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.controller.SecondhandProductControllerUc16WebMvcTest` | mockmvc-contract | UC16 | 2 | 2 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.controller.SecondhandProductControllerUc17WebMvcTest` | mockmvc-contract | UC17 | 1 | 1 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.controller.SecondhandTradeControllerUc18WebMvcTest` | mockmvc-contract | UC18 | 1 | 1 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.controller.SecondhandTradeControllerUc19WebMvcTest` | mockmvc-contract | UC19 | 1 | 1 | 0 | 0 | 0 | API_PASS |
| `com.segroup8.platform.controller.OrderControllerUc20WebMvcTest` | mockmvc-contract | UC20 | 1 | 1 | 0 | 0 | 0 | API_PASS |