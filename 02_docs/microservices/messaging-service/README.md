# messaging-service V1

`messaging-service` is the independent owner of chat conversations, chat messages, durable notifications, local access/block projections, and `/ws/realtime`. It is a Java 17-compatible Spring Boot 3.3.4 module and runs independently from `backend` on port `8084` by default.

## Run locally

Provision `messaging_db` and the `messaging_app` account according to `database-ownership.md`, then set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and an explicit `REALTIME_ALLOWED_ORIGIN_PATTERNS` value. Run:

```powershell
$env:JAVA_HOME='D:\java\IntelliJ IDEA 2025.2.1\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -B -f microservices/pom.xml -pl messaging-service -am spring-boot:run
```

Flyway applies `V1__create_messaging_tables.sql`. The default WebSocket origin is only `http://localhost:5174`; production must supply its exact HTTPS origin(s). JWTs use the shared `microservices/security-contract` verifier. No identity headers are trusted.

The Vite development proxy keeps `/api/chat/**`, `/api/notifications/**`, and `/ws/realtime` stable while forwarding them to port 8084. Other `/api/**` paths continue to use the monolith on port 8080. Production Nginx/container routing remains V3 work.

## Recovery model

Every message or notification is committed before best-effort WebSocket delivery (`CHAT_MESSAGE` and frontend-compatible `NOTIFICATION_CREATED`). A failed push does not roll back data. After reconnect, the existing client emits `REALTIME_RECONNECTED`; views recover through the conversation/message and notification REST queries. V1 intentionally has no cursor, ACK, outbox, retry, DLQ, or replay system.

## Governance compatibility

Access status is fail-close from `user_access_projection`. Block checks first require two authoritative directional rows in `user_block_projection`. When a row is missing, the explicit governance Port calls the current monolith block-check APIs with the actor's already-verified bearer token. If the compatibility source is unavailable or inconclusive, conversation creation and message sending return 503 and remain denied.

Existing WebSocket sessions can be disconnected through `RealtimePublisher.disconnectUser`. The `UserAccessChanged.v1` event trigger is deferred to V2.

## DOC-CODE GAP

Current: the monolith chat implementation reads `user`, `product`, `shop`, `secondhand_product`, and the `user_block` fact table directly.

Target: messaging reads only its five owned tables; resolved participant IDs and source snapshots enter through the conversation request/projections.

Migration strategy: backfill snapshots and projections with the controlled SQL migration, route V1 traffic to messaging, retain monolith tables for rollback, and use the governance compatibility Port until V2 maintains projections through versioned events.

Issue, PR, and review evidence: **MANUAL ACTION REQUIRED**. No synthetic management evidence was created.
