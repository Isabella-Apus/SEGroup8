# Cross-service calls (V3)

| Caller | Boundary | Authentication | Data crossing the boundary |
|---|---|---|---|
| Monolith domain producer | `POST /internal/events` | `X-Internal-Service-Token` + service identity | Versioned event envelope and historical snapshot |
| Messaging replay operator | `POST /internal/events/replay/{eventId}` | Separate operations token | event id, reason, trace id |
| Messaging block adapter | `POST /internal/blocks/check` | `X-Internal-Service-Token` + request identity headers | two directional block pairs and decisions |
| Client | `/api/chat/**`, `/api/notifications/**` | `security-contract` JWT | owned resource request |

No producer writes messaging tables and Messaging does not select producer,
identity, product, order, payment, refund, merchant, or governance fact tables.
