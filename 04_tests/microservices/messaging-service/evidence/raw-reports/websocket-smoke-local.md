# WebSocket smoke evidence

Date: 2026-08-31. The trace is deliberately redacted: no JWT or query token is
stored.

The messaging process was running on the local port `8084`; a static frontend
origin was served on `http://localhost:5174` so the browser supplied a real
Origin header.

| Case | Result |
|---|---|
| Valid JWT + allowed origin | `open`, then clean close |
| Missing token | rejected (`error`) |
| Invalid token | rejected (`error`) |
| Valid JWT + disallowed origin | rejected (`error`) |

The valid token was generated in memory with the configured local test secret;
it was not printed or persisted. This is a local process smoke test, not a
production-cluster deployment claim.
