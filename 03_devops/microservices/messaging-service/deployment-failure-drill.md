# Deployment failure drill

The required drill is a wrong `REALTIME_ALLOWED_ORIGIN_PATTERNS` value. The
expected execution is: deploy a known-good SHA, smoke-test a permitted origin,
deploy the invalid allow-list, observe readiness/smoke failure, verify Helm
atomic rollback and pod events, restore the allow-list, then verify readiness,
WebSocket handshake, backlog consumption, and one notification per dedupe key.

This repository contains the atomic rollback and probe configuration. A local
execution is currently blocked because no Kubernetes cluster is configured on
the verification workstation (the chart and Helm static validation do pass);
see
`04_tests/microservices/messaging-service/fault-injection-report.md`.
