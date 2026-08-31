# messaging-service V2/V3 delivery

`messaging-service` owns chat conversations, messages, durable notifications, access/block projections, `/ws/realtime`, event Inbox/idempotency state, and its delivery Outbox. V2 keeps the V1 REST/recovery contract and adds reliable event-driven notification delivery without Kafka/RabbitMQ/Redis.

## Run locally

Provision `messaging_db` and `messaging_app`, then set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, explicit `REALTIME_ALLOWED_ORIGIN_PATTERNS`, `INTERNAL_SERVICE_TOKEN`, and the separately scoped `INTERNAL_OPERATIONS_TOKEN`. For governance fallback, set `IDENTITY_SERVICE_URL` and the service-only `IDENTITY_SERVICE_TOKEN` (the latter must never be a user JWT). Configure the monolith relay with its service token and `MESSAGING_SERVICE_URL`. Run:

```powershell
$env:JAVA_HOME='D:\java\IntelliJ IDEA 2025.2.1\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -B -f microservices/pom.xml -pl messaging-service -am spring-boot:run
```

Flyway applies V1 and `V2__reliable_event_messaging.sql`. Public APIs use JWTs from `security-contract`; internal APIs reject user JWTs and require environment-injected service credentials. Replay additionally requires the operations credential. Tokens and notification/message bodies are not application-log fields.

The Vite development proxy keeps `/api/chat/**`, `/api/notifications/**`, and `/ws/realtime` stable while forwarding them to port 8084. Other `/api/**` paths continue to use the monolith on port 8080. Production Nginx now applies the same split: the frontend container sends only these paths to the `messaging` Service and keeps other APIs on `backend`.

## V2 event and recovery model

The current monolith producers write a complete v1 envelope into their own `outbox_event` in the same transaction as order/payment/refund/merchant/secondhand/governance state. A scheduled relay posts the envelope to private `/internal/events`; Messaging first establishes the unique Inbox boundary, then commits Notification plus WebSocket delivery Outbox. Inbox and delivery workers use bounded retry/backoff and persistent `DLQ` state. Offline WebSocket delivery stays pending while REST remains authoritative recovery.

`POST /internal/notifications` is a compatibility path, not the core producer path. It requires service identity and a `dedupeKey`; `idempotency_record`, Notification constraints, and the delivery Outbox prevent duplicates. Replay resets a stored Inbox event, preserves eventId/dedupe rules, and appends a durable audit task. `GET /internal/delivery/{dedupeKey}` reports persisted/pending/delivered/DLQ state without exposing content or credentials.

## Governance compatibility

Access status is fail-close from `user_access_projection`. Block checks first require two authoritative directional rows in `user_block_projection`. A migrated `source_version=0` inactive row is only a bootstrap snapshot and is not treated as authoritative permission. When a row is missing or non-authoritative, the explicit governance Port calls identity-governance's `POST /internal/blocks/check` with both directional pairs, `X-Internal-Service-Token`, request identity headers, explicit connect/read timeouts, and bounded retry. The authenticated user's JWT is never forwarded to identity. If the compatibility source is unavailable or inconclusive, conversation creation and message sending return 503 and remain denied.

`UserAccessChanged.v1` updates `user_access_projection` by source version. BANNED/DISABLED commits trigger `disconnectUser`; subsequent REST and WebSocket handshakes remain fail-close. Block behavior stays on the V1 projection plus governance fallback because MS-06 defines no new block event.

## DOC-CODE GAP

Current: the monolith chat implementation reads `user`, `product`, `shop`, `secondhand_product`, and the `user_block` fact table directly.

Target: messaging reads only its five owned tables; resolved participant IDs and source snapshots enter through the conversation request/projections.

Migration strategy: retain the V1 data transfer and monolith rollback tables, apply V2 Flyway migration, enable producer Outbox as the default notification path, then run relay and Messaging workers. The old local Notification implementation is available only when `app.messaging.event-notifications-enabled=false` and is **LEGACY ROLLBACK ONLY**.

V3 adds an immutable SHA-tagged Dockerfile, Helm Deployment/Service/ConfigMap,
atomic rollout, health groups, version info, Micrometer metrics, correlation
logging, and CI gates in the existing pipeline. The Docker build and Helm
lint/template checks pass locally; Kubernetes rollout, production smoke, and
failure-drill execution require the configured cluster and remain manual
actions. See the V3 evidence directory.

Issue, PR, and review evidence: **MANUAL ACTION REQUIRED**. No synthetic management evidence was created.
