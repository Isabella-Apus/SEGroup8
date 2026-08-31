# Scenario C live evidence (2026-08-30)

This record contains identifiers only; credentials, JWTs, and message bodies are
intentionally omitted.

| Field | Observed value |
|---|---|
| orderId | 51 |
| paymentStatus | 1 (success) |
| eventId | `bf9aa3e6-1062-4eb0-af12-b04656f3de8a` |
| dedupeKey | `PaymentCompleted.v1:51:2:PAID:2881276c` |
| traceId | `ms06-v2-live-fault-trace` |
| producer outbox while Messaging stopped | same event, `RETRY`, retryCount 3/4 observed |
| producer outbox after restart | same event, `PUBLISHED`, retryCount 5 |
| inbox after restart | same event, `PROCESSED`, retryCount 0 |
| notification | id 54; count for the dedupe key = 1 |
| messaging delivery outbox | one row, `PENDING` (recipient offline) |
| recovery | REST notification recovery remains available |

The order and payment responses were successful while Messaging was stopped;
the event was not manually recreated. This is the real stop/restart evidence
used to close V2 Scenario C.
