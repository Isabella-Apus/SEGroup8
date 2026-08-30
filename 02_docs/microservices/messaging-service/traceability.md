# MS-06 V2 implementation traceability

| Requirement | Code | Automated evidence |
|---|---|---|
| Seven event contracts/envelope | backend and Messaging `event/EventTypes.java`, `EventEnvelope.java` | `ReliableMessagingIntegrationTest.allSevenContractsUseOneEnvelopeAndSnapshotWithoutSourceQueries` plus producer service tests |
| Atomic producer Outbox | `ProducerOutboxService`, business `@Transactional` methods, producer schema | `ProducerOutboxFailureIsolationIntegrationTest` |
| Inbox/eventId/dedupe | `InboxEventService`, V2 migration, `NotificationService` | reliable integration duplicate/replay tests; MySQL constraint test |
| Internal notification idempotency/auth | `InternalServiceInterceptor`, `InternalNotificationService` | internal auth/dedupe/conflict integration test |
| Retry/DLQ/replay/audit | `InboxWorker`, `InboxEventService.replay`, audit delivery Outbox | retry-success/DLQ and repeated replay tests |
| Reliable WebSocket delivery | `DeliveryOutboxService`, `DeliveryWorker` | offline/success/failure/DLQ integration test |
| Access projection/session disconnect | `EventHandler.applyUserAccess`, V1 realtime service | projection test and actual WebSocket event-disconnect/reconnect rejection test |
| Source unavailable/snapshot | `EventHandler` | seven-contract historical snapshot test |
| Database boundary | owned migrations/config only | `BoundaryTest`; real MySQL foreign-schema denial |
| Failure isolation | producer relay and durable state machines | real payment with unreachable Messaging remains committed and Outbox enters RETRY; Messaging receive/recovery is covered independently. A single automated stop → backlog → restart scenario using the same business event remains PARTIAL |

V3 Docker/Helm/CI/CD/final observability/deployment-drill evidence is intentionally absent.
