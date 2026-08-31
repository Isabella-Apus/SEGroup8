# Migration version report

## Versioned schema

| Version | File | Status |
|---|---|---|
| V1 | `microservices/messaging-service/src/main/resources/db/migration/V1__create_messaging_tables.sql` | Implemented and validated on H2 MySQL mode and real MySQL 8.0 |
| V2 | `microservices/messaging-service/src/main/resources/db/migration/V2__reliable_event_messaging.sql` | Implemented and validated on H2 MySQL mode and real MySQL 8.0 |

V1 creates all five owned tables from an empty `messaging_db`. It does not load the monolith `schema.sql` and contains no `DROP`, `TRUNCATE`, cross-schema query, or seed data.

V2 adds `inbox_event`, `idempotency_record`, and Messaging `outbox_event`, plus only the two Notification dedupe indexes required by MS-06. It introduces no broker, producer schema reference, destructive statement, or V3 deployment asset. The real MySQL test verifies eight tables, unique eventId/dedupeKey enforcement, persisted RETRY/DLQ and delivery states, and foreign-schema denial.

Existing monolith databases require the non-destructive operator script `sql/ms06-v2-producer-outbox.sql` before enabling V2 producers. New databases receive the same table from backend `schema.sql`. The producer table remains in the producer schema so business mutation and Outbox insert share one local transaction.

## Data transfer

`data-migration-from-monolith.sql` is an operator-run, non-destructive transfer from `segroup8_platform` to a pre-migrated, empty `messaging_db`. It preserves IDs, timestamps, read state, relationships, source data, and minimal user/block projections. It never deletes source data.

Before execution, stop Chat/Notification writes or establish a documented maintenance window. Run the preflight queries, execute the transaction using a temporary migration principal that can read the source and write the target, then run every reconciliation query. Do not grant those cross-schema permissions to `messaging_app`.

Acceptance requires equal row counts and `MAX(id)` values for the three business tables, zero orphan messages, and zero null participant/user IDs. Keep monolith tables unchanged as the rollback baseline. Routing rollback points the three paths back to the monolith; target rows are retained for investigation and must not be automatically deleted.

## DOC-CODE GAP

Current: monolith tables have no participant display snapshot columns and block facts store only active blocks.

Target: conversations retain minimal participant snapshots, and block projections can represent both active and known-inactive directional decisions.

Migration strategy: populate conversation snapshots from users during the controlled transfer; copy active blocks; add known-inactive decisions for participant directions of migrated conversations. Unknown new pairs use the governance compatibility Port and fail closed if it is unavailable.
