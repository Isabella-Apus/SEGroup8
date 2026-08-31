# MS-06 V2 event contract

All types below use `eventVersion: 1` and the common envelope documented in `cross-service-calls.md`.

| Event type | Producer now | Required business snapshot | Consumer result |
|---|---|---|---|
| `NotificationRequested.v1` | Monolith notification compatibility producer | recipient(s), displayTitle, displayText, dedupeKey; business/source and target when available | Notification + delivery Outbox |
| `OrderStatusChanged.v1` | Monolith order module | recipient(s), orderId/orderNo, newStatus/result, historical display, target, dedupeKey | Notification + delivery Outbox |
| `PaymentCompleted.v1` | Monolith payment path in `OrderServiceImpl` | recipient(s), split order ID/no, paid amount/result, historical display, target, dedupeKey | Notification + delivery Outbox |
| `RefundCompleted.v1` | Monolith order/refund paths | recipient(s), order ID/no, REFUNDED result, historical display, target, dedupeKey | Notification + delivery Outbox |
| `MerchantApproved.v1` | Monolith merchant approval | applicant, application/shop identifiers, approved display snapshot, target, dedupeKey | Notification + delivery Outbox |
| `SecondhandTradeSettled.v1` | Monolith auction settlement | buyer/seller, trade/order/product IDs, price, historical display/target, dedupeKey | Per-recipient Notifications + delivery Outboxes |
| `UserAccessChanged.v1` | Monolith user governance | userId, ACTIVE/NORMAL/BANNED/DISABLED status, role if known, monotonic version | Versioned projection upsert; banned/disabled session disconnect |

Missing optional historical strings are rendered as `[Historical notification]` / `[Historical details unavailable]`. Missing recipients or required access fields fail processing and enter Retry/DLQ. Event snapshots can be consumed after source deletion/unavailability; Messaging performs no source lookup.

Idempotency boundaries are separate: Inbox unique `eventId`; Notification unique `event_id + user_id` and business `dedupe_key`; `/internal/notifications` request hash in `idempotency_record`; delivery task unique delivery dedupe key.
