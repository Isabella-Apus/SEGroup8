# Service boundary

## Owned behavior

- Authenticate Chat/Notification REST and realtime handshakes with `security-contract`.
- Authorize conversation participants and notification owners.
- Create/list conversations using participant and source snapshots.
- Persist/list/read messages and notifications.
- Enforce local access and bidirectional block decisions, with fail-close fallback.
- Manage realtime sessions and durable-Outbox `CHAT_MESSAGE` / `NOTIFICATION_CREATED` delivery after commit.
- Accept the seven MS-06 v1 event contracts through a transport-neutral envelope and private HTTP ingress.
- Own Inbox processing, event/dedupe idempotency, retry/DLQ/replay, replay audit backlog, and WebSocket delivery Outbox.

## Owned data

`chat_conversation`, `chat_message`, `notification`, `user_access_projection`, `user_block_projection`, `inbox_event`, `idempotency_record`, and Messaging `outbox_event` in `messaging_db`.

## Cross-service boundary

Messaging does not query or own users, products, shops, secondhand products, orders, payments, refunds, merchant applications, or the governance block fact table. Product/shop/secondhand resolution is performed by the caller; the core receives `targetUserId`, `sourceType`, `sourceId`, and optional `sourceTitle` snapshot. The compatibility block adapter calls APIs, never a foreign schema.

Producers never insert into `messaging_db`; Messaging never selects producer/identity schemas or producer Outbox. The monolith owns a separate producer `outbox_event` in `segroup8_platform` and relays envelopes over authenticated HTTP. Event payload snapshots carry recipient, business/source identifiers, historical display text/title, target path, and dedupe key, so consumers do not call source services. Docker, Helm, production routing, CI/CD, final metrics/probes, and deployment drills remain V3.

## Ban handling

REST and new WebSocket connections call `AccessPolicy.requireActive`. Missing projection state is unavailable and fails closed; BANNED/DISABLED is forbidden. `UserAccessChanged.v1` updates the versioned projection and, after commit, actively disconnects existing sessions.

## DOC-CODE GAP

Current: producer modules remain in the monolith and some source methods still call the `NotificationService` facade.

Target: producer business transactions do not synchronously store/push Messaging notifications.

Migration strategy: production `NotificationServiceImpl` maps residual generic calls to `NotificationRequested.v1` producer Outbox. Domain facts use their named events directly. The local mapper/push implementation is guarded by an off-by-default rollback flag and is not the V2 normal path.
