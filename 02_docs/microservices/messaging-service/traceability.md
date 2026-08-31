# MS-06 V2/V3 traceability

| Requirement | Code evidence | Test/runtime evidence | Status |
|---|---|---|---|
| Seven event contracts/envelope | `event/EventTypes.java`, `EventEnvelope.java` | reliable integration and producer tests | PASS |
| Atomic Producer Outbox | `ProducerOutboxService` and transactional producers | producer failure-isolation test; Scenario C live record | PASS |
| Inbox/eventId/dedupe | `InboxEventService`, V2 migration, notification constraints | replay and MySQL migration tests | PASS |
| Internal authentication | `InternalServiceInterceptor` | internal API integration tests | PASS |
| Retry/DLQ/replay/audit | `InboxWorker`, replay service, audit delivery Outbox | retry/DLQ/replay integration tests | PASS |
| Reliable delivery Outbox | `DeliveryOutboxService`, `DeliveryWorker` | delivery success/failure/offline tests | PASS |
| Access projection/session disconnect | `EventHandler.applyUserAccess`, realtime service | projection and disconnect tests | PASS |
| Snapshot/no source queries | `EventHandler` payload-only processing | seven-contract historical snapshot test; boundary scan | PASS |
| Database boundary | owned migrations and authenticated APIs | BoundaryTest and MySQL permission denial | PASS |
| Failure isolation Scenario C | Producer relay → Messaging restart → Inbox → Notification | `evidence/raw-reports/scenario-c-live.md`: order/payment success, same event PROCESSED, notification count 1 | PASS |
| Immutable Docker image | messaging Dockerfile and CI SHA tag | final local candidate `segroup8/messaging:sha-684aff9e87664ebb41d9844cacbfd8bdf2dc60b3`, image ID `sha256:b0c0bf155725efdd0c6ceff3a98ce1bbddca9f288c3f478f34cd197ad27821d6`, JAR SHA `0467c321fc551b2024a6e18f112faf96912094a7f0b78609a579e0ca18d4c165` | PASS |
| Helm/atomic rollback | messaging templates and `deploy-k3s.sh --atomic` | local `helm lint` and `helm template` pass; cluster rollout not run | MANUAL ACTION REQUIRED |
| Health/info/metrics/logging | application health groups, `MessagingMetrics`, `TraceContextFilter` | messaging Maven verification | PASS |
| Deployment failure drill | wrong-origin procedure documented | no local Kubernetes runtime | MANUAL ACTION REQUIRED |
