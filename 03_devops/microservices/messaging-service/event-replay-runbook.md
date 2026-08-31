# Event replay runbook

Find failures with:

```sql
select event_id,event_type,status,retry_count,last_error,trace_id
from inbox_event where status='DLQ' order by id;
select event_id,dedupe_key,status,retry_count,last_error
from outbox_event where status in ('RETRY','DLQ');
```

Replay with the operations credential:

```bash
curl -X POST "$MESSAGING_URL/internal/events/replay/$EVENT_ID" \
  -H "X-Internal-Service-Token: $INTERNAL_OPERATIONS_TOKEN" \
  -H "X-Service-Identity: ops" -H "X-Trace-Id: $TRACE_ID"
```

Confirm `inbox_event.status=PROCESSED`, inspect the replay audit Outbox row,
and verify `select count(*) from notification where dedupe_key=...` remains
one. Replay resets processing state only; eventId and dedupeKey constraints
remain active.
