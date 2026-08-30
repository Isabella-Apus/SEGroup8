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

The application never creates databases or users. Flyway creates only the five V1 tables and its schema-history table inside the already-provisioned schema.

## Tables

| Table | Purpose |
|---|---|
| `chat_conversation` | Participant relationship, source and participant display snapshots, last-message state |
| `chat_message` | Durable message content, sender/receiver, read state, creation time |
| `notification` | Durable user-owned notifications and minimal nullable V2 correlation reservations |
| `user_access_projection` | Minimal access status, role and display projection |
| `user_block_projection` | Versioned directional blocked/allowed decision |

There are no foreign keys to another schema and no runtime cross-schema joins. The MySQL migration test creates a second isolated schema and proves the messaging test account receives an SQL permission error when selecting it.
