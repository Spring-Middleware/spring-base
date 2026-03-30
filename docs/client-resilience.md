# Client Resilience (Circuit Breaker, Bulkhead and Connection Parameters)

TL;DR: The middleware provides configurable resilience for declarative clients through circuit-breakers (Resilience4j), bulkheads (semaphores) and connection parameters (timeouts, max connections, retries, backoff). Configure via the `@MiddlewareContract` / `@MiddlewareCircuitBreaker` annotations or via application properties.

## Concepts

- Circuit Breaker: prevents cascading failures by opening when error rate exceeds a threshold and routing calls through Resilience4j states (CLOSED, OPEN, HALF_OPEN).
- Bulkhead: per-cluster concurrency limits implemented with a semaphore to prevent resource exhaustion.
- Connection parameters: HTTP client timeouts, connection pool size, retries and retry backoff used when creating the internal WebClient.

## How it works in the codebase (high level)

- A declarative client interface is annotated with `@MiddlewareContract` (module `api`) and its connection/circuit settings are read at startup by `ProxyClientResilienceConfigurator` (module `app`).
- When a client call executes, `ProxyClient` builds a `ProxyConnectionTask` and then:
  - looks up a bulkhead semaphore in `ClusterBulkheadRegistry` and acquires/release it around the call, and
  - looks up/creates a Resilience4j `CircuitBreaker` from `ClusterCircuitBreakerRegistry` and executes the call under the breaker when enabled.

Files to inspect for implementation details:

- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/ProxyClient.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/ClusterCircuitBreakerRegistry.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/ClusterBulkheadRegistry.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/MiddlewareCircuitBreakerParameters.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/MiddlewareClientConnectionParameters.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/config/ProxyClientResilienceConfigurator.java`
- `parent/app/src/main/java/io/github/spring/middleware/client/proxy/MethodMetaDataExtractor.java`

## Configure via annotations

The primary annotations are in the `api` module. Use `@MiddlewareContract` at the interface level and `@MiddlewareCircuitBreaker` at the method level to override interface defaults.

Example: interface-level configuration (uses property placeholders)

```java
@MiddlewareContract(
  name = "product",
  connection = @MiddlewareContractConnection(
    timeout = "${middleware.client.product.connection.timeout:30000}",
    maxConnections = "${middleware.client.product.connection.max-connections:50}",
    maxConcurrentCalls = "${middleware.client.product.connection.max-concurrent-calls:200}",
    maxRetries = "${middleware.client.product.connection.max-retries:3}",
    retryBackoffMillis = "${middleware.client.product.connection.retry-backoff-millis:1000}"
  ),
  circuitBreaker = @MiddlewareCircuitBreaker(
    enabled = "${middleware.client.product.circuit-breaker.enabled:true}",
    failureRateThreshold = "${middleware.client.product.circuit-breaker.failure-rate-threshold:50}",
    waitDurationInOpenStateMs = "${middleware.client.product.circuit-breaker.wait-duration-in-open-state-ms:10000}",
    slidingWindowSize = "${middleware.client.product.circuit-breaker.sliding-window-size:20}",
    minimumNumberOfCalls = "${middleware.client.product.circuit-breaker.minimum-number-of-calls:10}",
    permittedNumberOfCallsInHalfOpenState = "${middleware.client.product.circuit-breaker.permitted-number-of-calls-in-half-open-state:3}",
    statusShouldOpenBreaker = {"${middleware.client.product.circuit-breaker.status-should-open-breaker:5xx}"},
    statusShouldIgnoreBreaker = {"${middleware.client.product.circuit-breaker.status-should-ignore-breaker:4xx}"}
  )
)
public interface ProductClient {
  @GetMapping("/api/v1/products/{id}")
  ProductDto getProduct(@PathVariable("id") String id);
}
```

Method-level override example (overrides the interface defaults)

```java
@GetMapping("/health")
@MiddlewareCircuitBreaker(
  enabled = "true",
  failureRateThreshold = "30",
  minimumNumberOfCalls = "5",
  slidingWindowSize = "10",
  waitDurationInOpenStateMs = "5000",
  statusShouldOpenBreaker = {"5xx"},
  statusShouldIgnoreBreaker = {"4xx"}
)
HealthInfo health();
```

Notes:
- Annotation attributes are declared as Strings to allow Spring property placeholders. They are resolved at runtime by `ProxyClientResilienceConfigurator` and `MethodMetaDataExtractor`.
- Method-level `@MiddlewareCircuitBreaker` overrides interface-level circuit-breaker settings for that method only.

## Configure via properties

You can provide properties per-client. Example YAML (application.yml):

```yaml
middleware:
  client:
    defaultMaxConcurrent: 25
    registry-endpoint: "http://registry.local"
    product:
      enabled: true
      connection:
        timeout: 20000
        maxConnections: 30
        maxConcurrentCalls: 80
      circuit:
        enabled: true
        failureRateThreshold: 40
        minimumNumberOfCalls: 8
        slidingWindowSize: 16
        waitDurationInOpenStateMs: 8000
```

Equivalent properties:

```
middleware.client.defaultMaxConcurrent=25
middleware.client.registry-endpoint=http://registry.local
middleware.client.product.connection.timeout=20000
middleware.client.product.circuit.enabled=true
middleware.client.product.circuit.failureRateThreshold=40
```

Important properties used by the codebase

- `middleware.client.defaultMaxConcurrent` — default bulkhead (semaphore) size used by `ClusterBulkheadRegistry` when no override is provided.
- `middleware.client.<clientName>.connection.*` — connection parameters used to create the HTTP client for the named middleware client.
- `middleware.client.<clientName>.circuit.*` — circuit-breaker properties (when provided) that can be resolved into `MiddlewareCircuitBreakerParameters`.

## Defaults (from code)

The client uses a default connection parameter set when none is configured. These defaults come from `MiddlewareClientConnectionParameters.defaultParameters()`:

- timeout: 30000 ms
- maxConnections: 50
- maxConcurrentCalls: 200
- maxRetries: 3
- retryBackoffMillis: 1000 ms

`ClusterBulkheadRegistry` also reads a property default `middleware.client.defaultMaxConcurrent` (the code default used is typically 30 if the property is absent).

## Status expressions and error handling

Circuit-breaker status expressions are simple patterns used by the `ClusterCircuitBreakerRegistry` to decide when a status opens or is ignored by the breaker. Typical examples:

- `5xx` — any 5xx HTTP status should count as a failure to open the breaker
- `4xx` — treat client errors as ignored (do not increment failure counters)

The registry implements logic similar to `matches(status, expression)` to support patterns like `5xx`, `404`, etc. Additionally, exceptions thrown by the underlying call may be inspected and mapped to open/ignore decisions.

## Implementation notes & known issue

- In `MiddlewareCircuitBreakerParameters` there is a field with a typo named `enanbled` instead of `enabled`. The runtime code reads this field (`isEnanbled()` generated by Lombok). We recommend renaming it to `enabled` (and updating any code that references it) in a follow-up PR, but the documentation below references the current field names when quoting code.

## Tuning & recommendations

- Start with conservative defaults for production: a low `maxConcurrentCalls` per remote system and modest retry/backoff values.
- Use circuit-breaker `minimumNumberOfCalls` and `slidingWindowSize` to avoid noisy opens on low traffic endpoints.
- Configure `statusShouldOpenBreaker` to map service error codes consistently across services (e.g. treat 5xx as server failures).
- If you rely heavily on long-running requests, increase `timeout` and ensure your bulkhead `maxConcurrentCalls` is sized to avoid thread/resource starvation.

## Reference snippets (where to read more in the code)

- `ProxyClient.executeCall(...)`: acquires a semaphore from `ClusterBulkheadRegistry#getOrCreate(...)`, runs the task and releases the semaphore in `finally`.
- `ClusterBulkheadRegistry#getOrCreate(String clusterName, Integer overrideMaxConcurrent)`: returns a `Semaphore` per cluster.
- `ClusterCircuitBreakerRegistry#getOrCreate(String breakerName, MiddlewareCircuitBreakerParameters)`: creates a Resilience4j `CircuitBreaker` with `CircuitBreakerConfig` built from the parameters.
- `ProxyClient.recreateHttpClient()` / `ProxyClient.configureHttpClient()`: create/reconfigure the internal `WebClient` using `MiddlewareClientConnectionParameters`.
- `MiddlewareClientConnectionParameters.defaultParameters()` — shows default connection values.

## FAQ

Q: Where do I place per-client configuration?  
A: You can put client-specific settings under `middleware.client.<clientName>.*` (YAML or properties) and/or use placeholders in `@MiddlewareContract` annotations.

Q: Can I disable circuit-breaker for a specific method?  
A: Yes — use `@MiddlewareCircuitBreaker(enabled = "false")` on the method.

Q: Can I apply bulkhead per-cluster and still tweak per-method?  
A: The framework uses per-cluster semaphores with an optional method-level override for `maxConcurrentCalls` resolved from annotation placeholders.

---

If you want, I can also add a small unit test example that uses a mocked `ProxyClient`/`ClusterCircuitBreakerRegistry` to assert that the breaker opens after configured failures. I can also create a short PR that renames the typo `enanbled` to `enabled` across the codebase and update the annotations accordingly.

