# Database ownership

## Database and account

- Schema: `messaging_db`
- Runtime/migration account: `messaging_app`
- Connection defaults: `jdbc:mysql://127.0.0.1:3306/messaging_db`, overridden through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- The account must receive privileges only on `messaging_db.*`; it must have no grants on `segroup8_platform` or other service schemas.

Example DBA provisioning (replace the password through the operator's secret mechanism; do not commit it):

```sql
CREATE DATABASE IF NOT EXISTS messaging_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'messaging_app'@'%' IDENTIFIED BY '<secret-from-vault>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON messaging_db.* TO 'messaging_app'@'%';
```

The application never creates databases or users. Flyway V1/V2 creates only the eight owned business/reliability tables and schema history inside the provisioned schema.

## Tables

| Table | Purpose |
|---|---|
| `chat_conversation` | Participant relationship, source and participant display snapshots, last-message state |
| `chat_message` | Durable message content, sender/receiver, read state, creation time |
| `notification` | Durable user-owned notifications and minimal nullable V2 correlation reservations |
| `user_access_projection` | Minimal access status, role and display projection |
| `user_block_projection` | Versioned directional blocked/allowed decision |
| `inbox_event` | Unique eventId boundary, serialized envelope, processing/retry/DLQ state |
| `idempotency_record` | Internal notification HTTP request hash and stable result by dedupeKey |
| `outbox_event` | Durable WebSocket delivery and replay-audit backlog |

Notification has unique event-recipient and business dedupe indexes. Inbox eventId, idempotency dedupeKey, and delivery event/dedupe IDs are unique. There are no foreign-schema keys/joins. The real MySQL 8 test migrates both versions, exercises these reliability constraints/states, and proves the Messaging account cannot read an isolated foreign schema.

The producer `segroup8_platform.outbox_event` is not a Messaging-owned table. It is committed with producer business state and is accessed only by the producer relay; Messaging never reads it.
