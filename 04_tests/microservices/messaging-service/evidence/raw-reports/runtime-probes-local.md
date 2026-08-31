# Messaging local runtime probes

Date: 2026-08-31. A standalone messaging process was started on port `8084`
with database and authentication settings injected through environment
variables. No backend process was required for these probes.

| Probe | HTTP result | Observed result |
|---|---:|---|
| `/actuator/health/liveness` | 200 | `UP` |
| `/actuator/health/readiness` | 200 | `UP` |
| `/actuator/info` | 200 | service `messaging-service`, version `v3-local`, commit `0bafb9d6` |
| `/actuator/metrics/messaging.websocket.connections.active` | 200 | meter available |
| `/actuator/metrics/messaging.events.backlog` | 200 | meter available |
| `/actuator/metrics/messaging.events.consume.failures` | 200 | meter available |
| `/actuator/metrics/messaging.websocket.push.failures` | 200 | meter available |
| `/actuator/metrics/messaging.events.retry.count` | 200 | meter available |

The process was stopped after the smoke checks. This evidence contains no JWT,
password, internal token, or message body.
