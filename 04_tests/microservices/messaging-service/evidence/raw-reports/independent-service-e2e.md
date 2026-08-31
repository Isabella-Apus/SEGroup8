# Independent service API E2E

Status: **PASS** for the local candidate image against an isolated MySQL 8.4 database.

- Run date: `2026-08-31` (Java 21 host, Docker Desktop Linux engine)

- Candidate image: `segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3` (`sha256:b0c0bf155725efdd0c6ceff3a98ce1bbddca9f288c3f478f34cd197ad27821d6`)
- Candidate JAR SHA-256: `0467c321fc551b2024a6e18f112faf96912094a7f0b78609a579e0ca18d4c165`
- Database: isolated `mysql:8.4.6`, not the monolith schema.
- Service API checks: route contract, actuator contract, JWT/API authorization, internal service authentication, idempotent notification endpoint, replay endpoint, delivery status endpoint, and WebSocket handshake.
- Projection rows were supplied locally for the main path; no producer database or monolith backend was used by the candidate service.
- Strict downstream stub `scripts/ci/strict-downstream-stub.mjs` was run on host port 18085. With one block projection row removed, the candidate called exactly `GET /api/report-block/block/check/1002` and `GET /api/report-block/block/blocked-by/1002`; the stub validated method, path, and Bearer credential and returned `{code:0,data:false}`. Three request pairs were observed during the API audit.
- The API audit then completed the conversation fallback path with HTTP 200. No request accepted an arbitrary method/path or missing credential.
- Public API audit result: 12/12 reviewed operations returned the expected success response; negative probes returned 401 (missing/invalid JWT and missing internal credential), 403 (non-participant), 400 (empty content), and 404 (missing notification).
- WebSocket audit result: valid JWT + allowed Origin handshook successfully; missing/invalid JWT and disallowed Origin were rejected.
- Database result after the run: Inbox event `final-audit-1788140721037` was `PROCESSED`; replay and HTTP idempotency checks left one notification per dedupe key; delivery rows remained durable `PENDING` while the test users were offline.
