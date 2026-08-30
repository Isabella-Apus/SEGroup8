# Migration version report

## Versioned schema

| Version | File | Status |
|---|---|---|
| V1 | `microservices/messaging-service/src/main/resources/db/migration/V1__create_messaging_tables.sql` | Implemented and validated on H2 MySQL mode and real MySQL 8.0 |

V1 creates all five owned tables from an empty `messaging_db`. It does not load the monolith `schema.sql` and contains no `DROP`, `TRUNCATE`, cross-schema query, or seed data.

## Data transfer

`data-migration-from-monolith.sql` is an operator-run, non-destructive transfer from `segroup8_platform` to a pre-migrated, empty `messaging_db`. It preserves IDs, timestamps, read state, relationships, source data, and minimal user/block projections. It never deletes source data.

Before execution, stop Chat/Notification writes or establish a documented maintenance window. Run the preflight queries, execute the transaction using a temporary migration principal that can read the source and write the target, then run every reconciliation query. Do not grant those cross-schema permissions to `messaging_app`.

Acceptance requires equal row counts and `MAX(id)` values for the three business tables, zero orphan messages, and zero null participant/user IDs. Keep monolith tables unchanged as the rollback baseline. Routing rollback points the three paths back to the monolith; target rows are retained for investigation and must not be automatically deleted.

## DOC-CODE GAP

Current: monolith tables have no participant display snapshot columns and block facts store only active blocks.

Target: conversations retain minimal participant snapshots, and block projections can represent both active and known-inactive directional decisions.

Migration strategy: populate conversation snapshots from users during the controlled transfer; copy active blocks; add known-inactive decisions for participant directions of migrated conversations. Unknown new pairs use the governance compatibility Port and fail closed if it is unavailable.
