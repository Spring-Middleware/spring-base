# Request / Response Logging

This document explains the logging configuration exposed by Spring Middleware and how it affects the built-in `RequestLoggingFilter`.

Configuration location
----------------------
All logging controls are available under the `middleware.log` prefix and are represented by the class `io.github.spring.middleware.filter.MiddlewareLogProperties`.

Quick summary of available properties (defaults):

- `middleware.log.request.enabled` (boolean) — default: `true` — enables request logging.
- `middleware.log.response.enabled` (boolean) — default: `true` — enables response logging.
- `middleware.log.responseTime.enabled` (boolean) — default: `false` — enables calculation and logging of response time.
- `middleware.log.exclude.url-patterns` (list of strings) — default: `[]` — Ant-style patterns to exclude from logging (e.g. `/actuator/**`).
- `middleware.log.apiKey` (String) — optional key that can be used to force logging for a specific request when provided in a request header (see "Forced logging" below).

How the `RequestLoggingFilter` uses these properties
--------------------------------------------------
The filter implementation is `io.github.spring.middleware.filter.RequestLoggingFilter`. Important points:

- The filter reads `middleware.log.request.enabled`. When `true` (default) the filter will log an informational message with the request line and (when possible) the request body.

- The filter reads `middleware.log.response.enabled`. When `true` (default) the filter will log an informational message with the response status and (when possible) the response body.

- Response time calculation is controlled by `middleware.log.responseTime.enabled`. Additionally the filter checks a runtime toggle coming from a `Context` key named `Response-Time-Log` (see `PropertyNames.RESPONSE_TIME_LOG`). The response time is measured with a `StopWatch` and appended to the response log message when enabled.

- `middleware.log.exclude.url-patterns` is used by the filter's `shouldNotFilter` method. If any configured pattern matches the request path the filter will skip logging for that request.

- The filter avoids reading request bodies for multipart requests; it checks the `MultipartResolver` and in that case does not attempt to wrap/read the request input stream.

- The filter also avoids logging actuator endpoints — it checks if the URI contains `actuator`.

- Logging is gated by the logger level: even if properties are enabled the filter still requires `requestResponseLog.isInfoEnabled()` to be true to emit messages.

Forced logging (incident debugging)
----------------------------------
A small but useful mechanism is available to force request/response logging for a single request without changing the application's logging configuration.

Mechanism summary:

- The filter infrastructure and the helper `LogRequestResponse` check for a header named `X-Logging-Key` (constant `PropertyNames.LOGGING_KEY`).
- If the header is present and its value matches the configured `middleware.log.apiKey`, logging for that request is "forced".
- Forced logging bypasses the `isInfoEnabled()` guard: if the logger level would otherwise prevent an INFO message from being emitted, `LogRequestResponse` will still emit the message by logging it at ERROR level internally when forced. This ensures the message is recorded even when the app runs at a higher log level (e.g. WARN or ERROR).

Why this is useful
- In production you often cannot change the global log level. When investigating a live incident you can request a caller (or a support tunnel) to include the secret key in the request header and collect the request/response pair for that single request.

Security and operational notes
- `middleware.log.apiKey` is sensitive and should be treated like a secret. Store it in a secure configuration source (secrets manager / env var) and rotate periodically.
- Do not expose the key in public clients or logs.
- Prefer using a time-limited, short-lived support token for incident debugging instead of a long-lived static value.
- For privacy and compliance, be careful when forcing body logging on sensitive endpoints (PII, credentials, payment data). Prefer `exclude.url-patterns` to omit sensitive paths.

Example usage

Set the key in configuration (YAML):

```yaml
middleware:
  log:
    apiKey: ${LOGGING_API_KEY:my-support-key}
```

When calling the service, add the header:

```
X-Logging-Key: my-support-key
```

If the value matches `middleware.log.apiKey`, the middleware will force emission of the request/response logs for that request even if the logger level would usually suppress them.

Notes about implementation
- The check is implemented in `LogRequestResponse.isLogForced()` which reads `Context.get(PropertyNames.LOGGING_KEY)` and compares to `middlewareLogProperties.getApiKey()`.
- The `LogRequestResponse` methods (e.g. `info`, `debug`, `warn`) will emit at the corresponding level if enabled, otherwise they fall back to logging at `error` when `isLogForced()` returns `true`. This guarantees the log is persisted by most logging stacks.

Notes about alternatives
- Instead of forcing logs via a header, another approach is to collect metrics and distributed traces and use them to locate problematic requests; forced logging is most helpful when you need the raw request/response payload for debugging.

Examples and best practice
- Use a dedicated support API or gateway that can inject the header securely for incident captures.
- Log only what is strictly necessary when forced. Consider redacting sensitive fields before emitting logs.

Where to look in the code
------------------------
- `parent/app/src/main/java/io/github/spring/middleware/log/LogRequestResponse.java`
- `parent/app/src/main/java/io/github/spring/middleware/filter/RequestLoggingFilter.java`
- `parent/commons/src/main/java/io/github/spring/middleware/config/PropertyNames.java`

---

## Related documentation

- `README.md` — project overview.
- `docs/architecture.md` — architecture and control/data plane.
- `docs/communication.md` — how request context propagates and client headers are forwarded.
- `docs/errors.md` — error model and how errors include request/trace metadata.
- `docs/client-security.md` — security for declarative clients.
- `docs/kafka.md` — Kafka module.
- `docs/redis.md` — Redis module.
- `docs/mongo.md` — Mongo module.
- `docs/jpa.md` — JPA module.
- `docs/rabbitmq.md` — RabbitMQ module.
- `docs/security.md` — HTTP security configuration.
- `docs/logging.md` — this document.
