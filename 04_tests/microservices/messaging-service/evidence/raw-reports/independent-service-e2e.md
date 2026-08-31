# Independent service API E2E

Status: **PASS** for the local candidate image against an isolated MySQL 8.4 database.

- Candidate image: `sha256:00da458d9e1622f6df8fcad691268b64c470e22e5ceea40d356f2db85299238a`
- Candidate JAR SHA-256: `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b`
- Database: isolated `mysql:8.4.6`, not the monolith schema.
- Service API checks: route contract, actuator contract, JWT/API authorization, internal service authentication, idempotent notification endpoint, replay endpoint, delivery status endpoint, and WebSocket handshake.
- Projection rows were supplied locally for the main path; no producer database or monolith backend was used by the candidate service.
- Strict downstream stub `scripts/ci/strict-downstream-stub.mjs` was run on host port 18085. With one block projection row removed, the candidate called exactly `GET /api/report-block/block/check/1002` and `GET /api/report-block/block/blocked-by/1002`; the stub validated method, path, and Bearer credential and returned `{code:0,data:false}`. Three request pairs were observed during the API audit.
- The API audit then completed the conversation fallback path with HTTP 200. No request accepted an arbitrary method/path or missing credential.
