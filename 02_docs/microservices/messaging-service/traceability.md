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
| Immutable Docker image | messaging Dockerfile and CI SHA tag | final local candidate `segroup8/messaging:sha-0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`, image ID `sha256:00da458d9e1622f6df8fcad691268b64c470e22e5ceea40d356f2db85299238a`, JAR SHA `3a874f2fe2e7fe16370dd7b6836047f7abd2fea419d97592bd3648803aca910b` | PASS |
| Helm/atomic rollback | messaging templates and `deploy-k3s.sh --atomic` | local `helm lint` and `helm template` pass; cluster rollout not run | MANUAL ACTION REQUIRED |
| Health/info/metrics/logging | application health groups, `MessagingMetrics`, `TraceContextFilter` | messaging Maven verification | PASS |
| Deployment failure drill | wrong-origin procedure documented | no local Kubernetes runtime | MANUAL ACTION REQUIRED |
