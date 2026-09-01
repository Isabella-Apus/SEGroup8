# Structured logging evidence

The candidate image emits one-line JSON records with `timestamp`, `level`, `logger`, `message`, and `service`. HTTP completion records emitted by `TraceContextFilter` include `method`, `path`, `status`, `durationMs`, and MDC `traceId`/`requestId`; domain logs add event/conversation/message/notification identifiers where applicable.

JWTs, query-string tokens, service credentials, passwords, and message bodies are not logged by the messaging filters. The JSON encoder is configured in `logback-spring.xml` and the HTTP completion path uses `request.getRequestURI()` (never the query string).
