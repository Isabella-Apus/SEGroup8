# Cross-service calls and producer mapping

## Transport-independent envelope

Every event uses one JSON envelope: `eventId`, exact `eventType`, `eventVersion=1`, `producer`, `aggregateType`, `aggregateId`, `occurredAt`, `traceId`, and `payload`. HTTP `POST /internal/events` is the current private transport; contracts do not contain HTTP fields.

| Current business call/site | V2 producer mapping | Snapshot / dedupe |
|---|---|---|
| Order status, shipment, cancellation, receipt and review notices in `OrderServiceImpl` | `OrderStatusChanged.v1` | recipient, orderId/orderNo, status/result, amount, display title/text, target path; stable semantic key |
| Successful `payMyOrder` | `PaymentCompleted.v1` | split order snapshots and seller recipient; one key per order/recipient/payment fact |
| Approved/automatic refund paths | `RefundCompleted.v1` | buyer/seller recipient, order snapshot and REFUNDED result |
| `MerchantApplicationServiceImpl.approve` | `MerchantApproved.v1` | applicant, application/shop snapshot and merchant target |
| Auction settlement in `SecondhandTradeServiceImpl` | `SecondhandTradeSettled.v1` | buyer/seller, trade/order/product snapshot, price, historical display fallback |
| `AdminUserServiceImpl.banUser/unbanUser` | `UserAccessChanged.v1` | userId, status, role/display snapshot and source version |
| Risk decisions, merchant rejection, bargain/bid notices, chat compatibility notices and other one-off notifications | `NotificationRequested.v1` through the producer facade | recipient, business/source IDs, display snapshot, target and dedupeKey |

The normal path is business transaction → producer `outbox_event` → commit → relay → Messaging Inbox. No core transaction calls Messaging HTTP. `app.messaging.event-notifications-enabled=false` exposes the old local mapper/push code only for rollback/testing and is **LEGACY ROLLBACK ONLY**.

## Authenticated calls

| Caller | Callee | Endpoint | Credential | Failure behavior |
|---|---|---|---|---|
| Monolith producer relay | Messaging | `POST /internal/events` | `INTERNAL_SERVICE_TOKEN` | Producer row remains RETRY/DLQ; business commit is unchanged |
| Approved compatibility services | Messaging | `POST /internal/notifications` | `INTERNAL_SERVICE_TOKEN` | Caller can query stable result by dedupeKey; core domains should use Outbox events |
| Operations | Messaging | `POST /internal/events/replay/{eventId}` | separate `INTERNAL_OPERATIONS_TOKEN` | Missing/invalid credential rejected; audit Outbox row and Inbox reset commit together |
| Internal support tooling | Messaging | `GET /internal/delivery/{dedupeKey}` | `INTERNAL_SERVICE_TOKEN` | Returns metadata only |
| Messaging block adapter | Current governance HTTP compatibility API | V1 block checks | verified caller JWT | Inconclusive/unavailable result fails closed |

Messaging never calls order/product/shop/payment databases or source APIs while consuming events. Producers never write `messaging_db`.
