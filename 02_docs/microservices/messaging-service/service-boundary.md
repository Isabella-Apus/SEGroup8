# Service boundary

## Owned behavior

- Authenticate Chat/Notification REST and realtime handshakes with `security-contract`.
- Authorize conversation participants and notification owners.
- Create/list conversations using participant and source snapshots.
- Persist/list/read messages and notifications.
- Enforce local access and bidirectional block decisions, with fail-close fallback.
- Manage realtime sessions and best-effort `CHAT_MESSAGE` / `NOTIFICATION` pushes after commit.

## Owned data

`chat_conversation`, `chat_message`, `notification`, `user_access_projection`, and `user_block_projection` in `messaging_db`.

## Explicitly outside V1

Messaging does not query or own users, products, shops, secondhand products, orders, payments, refunds, merchant applications, or the governance block fact table. Product/shop/secondhand resolution is performed by the caller; the core receives `targetUserId`, `sourceType`, `sourceId`, and optional `sourceTitle` snapshot. The compatibility block adapter calls APIs, never a foreign schema.

Business notification producers and all versioned events, inbox/outbox, retry, DLQ, replay, and delivery APIs are V2. Docker, Helm, production routing, CI/CD, final metrics/log delivery, and deployment drills are V3.

## Ban handling

REST and new WebSocket connections call `AccessPolicy.requireActive`. Missing projection state is unavailable and fails closed; `BANNED`/disabled state is forbidden. Session disconnect capability is implemented now. Event-driven disconnect triggering is deferred to V2.
