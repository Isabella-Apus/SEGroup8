# Independent service API E2E

Historical status: **PASS** for the previous local candidate image against an isolated MySQL 8.4 database.
The current source of truth is the dedicated workflow, which re-runs this contract on every relevant change
and uploads the complete runner-only report as an Actions artifact.

- Run date: `2026-08-31` (Java 21 host, Docker Desktop Linux engine)

- Candidate image: `segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3` (`sha256:b0c0bf155725efdd0c6ceff3a98ce1bbddca9f288c3f478f34cd197ad27821d6`)
- Candidate JAR SHA-256: `0467c321fc551b2024a6e18f112faf96912094a7f0b78609a579e0ca18d4c165`
- Database: isolated `mysql:8.4.6`, not the monolith schema.
- Service API checks: route contract, actuator contract, JWT/API authorization, internal service authentication, idempotent notification endpoint, replay endpoint, delivery status endpoint, and WebSocket handshake.
- Projection rows were supplied locally for the main path; no producer database or monolith backend was used by the candidate service.
- The dedicated CI E2E fixture uses `scripts/ci/strict-downstream-stub.mjs` as the identity-governance boundary. With bootstrap block rows at `source_version=0`, the candidate must call exactly `POST /internal/blocks/check` with both directional pairs, `X-Internal-Service-Token`, `X-Request-Id`, and `X-Idempotency-Key`; the stub rejects any `Authorization: Bearer` header.
- The API audit then completed the conversation fallback path with HTTP 200. No request accepted an arbitrary method/path or missing credential.
- Public API audit result: 12/12 reviewed operations returned the expected success response; negative probes returned 401 (missing/invalid JWT and missing internal credential), 403 (non-participant), 400 (empty content), and 404 (missing notification).
- WebSocket audit result: valid JWT + allowed Origin handshook successfully; missing/invalid JWT and disallowed Origin were rejected.
- Database result after the run: Inbox event `final-audit-1788140721037` was `PROCESSED`; replay and HTTP idempotency checks left one notification per dedupe key; delivery rows remained durable `PENDING` while the test users were offline.
