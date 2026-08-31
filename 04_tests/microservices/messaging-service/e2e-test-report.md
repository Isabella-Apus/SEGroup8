# V3 E2E report

UC24 and UC25 real business-path evidence remains under `v2-e2e-final/` and
passes with backend, Messaging, MySQL, and WebSocket. Scenario C’s real
stop/restart evidence is recorded in `evidence/raw-reports/scenario-c-live.md`.

No manual notification INSERT was used for the UC25 evidence. A second
production-cluster E2E run is required after Helm deployment and is not
pretended to have run locally.
