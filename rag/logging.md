# Request / Response Logging (RAG-Friendly)

## Quick Answer

**How do I configure request and response logging?**
All logging properties are configured under the `middleware.log` prefix in your `application.yml`.
By default, request and response logging are enabled, while response time logging is disabled.

**YAML Configuration:**
```yaml
middleware:
  log:
    request:
      enabled: true
    response:
      enabled: true
    responseTime:
      enabled: true
    exclude:
      url-patterns:
        - "/actuator/**"
        - "/health"
```

**Constraints:**
- The logger level for `requestResponseLog` MUST be set to `INFO` or higher for the logs to actually emit, even if `middleware.log.*.enabled` is `true`.
- The `RequestLoggingFilter` avoids reading request bodies for multipart requests.
- The `RequestLoggingFilter` automatically ignores any URI containing `actuator` regardless of the `exclude.url-patterns`.

---

## Log Exclusion

### How do I stop logging for specific endpoints?
You can exclude specific URL paths from being logged by defining Ant-style patterns under `middleware.log.exclude.url-patterns`.

**YAML Configuration required:**
```yaml
middleware:
  log:
    exclude:
      url-patterns:
        - "/api/v1/sensitive-data/**"
        - "/internal/metrics"
```

**Constraints:**
- Paths are matched using Ant-style pattern matching.
- Excluded paths bypass both request and response logging entirely.
- Endpoints containing `actuator` are always excluded automatically.

---

## Forced Incident Debugging

### How do I force logging for a single request in production?
You can conditionally force a request and its response to be logged (even if the logger level suppresses `INFO` logs) by configuring an API key and sending it via the `X-Logging-Key` HTTP header. 

**YAML Configuration required:**
```yaml
middleware:
  log:
    apiKey: ${LOGGING_API_KEY:my-secret-support-key}
```

**HTTP Request Example:**
```http
GET /api/v1/users/123 HTTP/1.1
Host: api.example.com
X-Logging-Key: my-secret-support-key
```

**Constraints:**
- When the `X-Logging-Key` header matches `middleware.log.apiKey`, the middleware forcibly logs the request and response internally at the `ERROR` level. This bypasses the normal `isInfoEnabled()` guard.
- Do NOT expose `middleware.log.apiKey` in public clients or logs; treat it as a sensitive secret.

---

## Response Time Logging

### How do I log the time taken by a request?
To measure and log the response time, you must enable `middleware.log.responseTime.enabled`.

**YAML Configuration required:**
```yaml
middleware:
  log:
    responseTime:
      enabled: true
```

**Java Context usage:**
The filter also checks a runtime toggle from a `Context` key named `Response-Time-Log` (`PropertyNames.RESPONSE_TIME_LOG`).
```java
import io.github.spring.middleware.config.PropertyNames;

// Example of dynamically checking or setting the context key 
// Context.put(PropertyNames.RESPONSE_TIME_LOG, "true");
```

**Constraints:**
- The response time is measured with a `StopWatch` and appended directly to the response log message.
- Response logging (`middleware.log.response.enabled`) MUST be active for the time calculation to be printed.
