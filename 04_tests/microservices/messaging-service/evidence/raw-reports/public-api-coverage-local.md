# Public/API route contract audit

Revision: `684aff9e87664ebb41d9844cacbfd8bdf2dc60b3`

Runtime controller/OpenAPI/reviewed manifest gate: **PASS**, 12 normalized operations matched.

| Operation | Success check | Negative/security check |
|---|---|---|
| GET `/api/chat/conversations` | 200 | missing JWT 401; invalid JWT 401 |
| POST `/api/chat/conversations` | 200 | empty/self/blocked validation covered by service tests |
| GET `/api/chat/conversations/{conversationId}/messages` | 200 | non-participant 403 |
| POST `/api/chat/conversations/{conversationId}/messages` | 200 | empty content 400 |
| GET `/api/notifications` | 200 | ownership enforced by service tests |
| POST `/api/notifications/{notificationId}/read` | 200 | missing notification 404 |
| POST `/api/notifications/read-all` | 200 | user ownership enforced by service tests |
| POST `/internal/events` | 200 | missing service credential 401 |
| POST `/internal/notifications` | 200 | missing service credential 401; dedupe contract covered by integration tests |
| POST `/internal/events/replay/{eventId}` | 200 | wrong non-operations credential 401 |
| GET `/internal/delivery/{dedupeKey}` | 200 | service credential required |
| GET `/ws/realtime` handshake | success | invalid/missing JWT and invalid Origin rejected |

`publicApiCoverage = 12/12` for the reviewed route manifest. HTTP response assertions used machine-readable `code` values and omitted JWTs, service tokens, and message bodies from evidence.
