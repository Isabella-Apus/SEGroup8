# Operations runbook

1. Check `/actuator/health/liveness`, `/actuator/health/readiness`, and
   `/actuator/info`.
2. Check `/actuator/metrics/messaging.events.backlog`,
   `messaging.events.consume.failures`, `messaging.websocket.push.failures`,
   `messaging.events.retry.count`, and
   `messaging.websocket.connections.active`.
3. Inspect `inbox_event` and `outbox_event` for `RETRY`/`DLQ`; do not delete
   rows while diagnosing.
4. Verify `GET /internal/delivery/{dedupeKey}` with the service token.
5. Restart through `helm upgrade --install --atomic --wait`; never bypass the
   readiness probes.

Credentials and JWTs must never be copied into logs or evidence.

Production deploys must provide the non-secret
`REALTIME_ALLOWED_ORIGIN_PATTERNS` variable; an empty value or `*` is rejected
by the deployment script. `IDENTITY_SERVICE_URL` is optional for the governance
fallback.
