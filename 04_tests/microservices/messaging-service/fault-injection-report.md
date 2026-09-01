# V3 fault-injection report

## Real Scenario C (completed before V3 packaging)

Messaging was stopped as a real process while the real backend executed order
and payment. The business calls succeeded; the Producer Relay retried the same
event until Messaging restarted. After restart the Inbox reached `PROCESSED`,
one notification was persisted, and the delivery Outbox remained `PENDING`
because the recipient was offline. Replaying the event is covered by the
reliable integration test and retains a single notification by dedupe key.

Raw identifiers and database observations are in
`evidence/raw-reports/scenario-c-live.md`; no JWT, secret, or message body is
stored.

## Deployment drill

The wrong-origin Helm drill is configured but not executed because no
Kubernetes cluster is configured on this workstation. Docker build and Helm
lint/template are independently PASS; the rollout, readiness failure,
rollback, and recovery steps remain **MANUAL ACTION REQUIRED**, not a PASS
claim.
