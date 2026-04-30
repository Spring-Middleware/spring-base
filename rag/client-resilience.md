# Client Resilience (RAG-Friendly)

## Quick Answer

**How do I configure Circuit Breaker and Bulkhead resilience in a Declarative Client?**
Combine `@MiddlewareContract` with `@MiddlewareCircuitBreaker` and apply them to your proxy client interface.

**Java code:**
```java
import io.github.spring.middleware.annotation.client.MiddlewareContract;
import io.github.spring.middleware.annotation.client.MiddlewareContractConnection;
import io.github.spring.middleware.annotation.client.MiddlewareCircuitBreaker;

@MiddlewareContract(
  name = "product",
  connection = @MiddlewareContractConnection(
    timeout = "${middleware.client.product.connection.timeout:30000}",
    maxConnections = "${middleware.client.product.connection.max-connections:50}",
    maxConcurrentCalls = "${middleware.client.product.connection.max-concurrent-calls:200}"
  ),
  circuitBreaker = @MiddlewareCircuitBreaker(
    enabled = "${middleware.client.product.circuit-breaker.enabled:true}",
    failureRateThreshold = "${middleware.client.product.circuit-breaker.failure-rate-threshold:50}",
    statusShouldOpenBreaker = {"5xx"},
    statusShouldIgnoreBreaker = {"4xx"}
  )
)
public interface ProductClient {
    
    @GetMapping("/api/v1/products/{id}")
    ProductDto getProduct(@PathVariable("id") String id);
}
```

**Constraints:**
- Resilience features (Circuit breaker, connection pool, semaphore Bulkhead) are read at startup via `ProxyClientResilienceConfigurator`.
- Method-level `@MiddlewareCircuitBreaker` annotations completely override the interface-level circuit-breaker settings for that particular method.
- The `type` name in `@MiddlewareContract` acts as the `clusterName`.

---

## Configuration using Properties

### Can I override resilience parameters via YAML properties instead of annotations?
Yes, using `middleware.client.<clientName>.connection.*` and `middleware.client.<clientName>.circuit.*` properties.

**YAML Configuration:**
```yaml
middleware:
  client:
    defaultMaxConcurrent: 25
    product:
      connection:
        timeout: 20000
        maxConcurrentCalls: 80
      circuit:
        enabled: true
        failureRateThreshold: 40
        waitDurationInOpenStateMs: 8000
```

**Constraints:**
- By code default, `MiddlewareClientConnectionParameters` uses 30000ms timeout and `200` max concurrent calls if otherwise not specified.
- The bulkhead (Semaphore size per cluster) falls back to `middleware.client.defaultMaxConcurrent` if not overridden explicitly per client.

---

## Circuit Breaker Behavior

### How does the circuit breaker decide when to open?
It leverages `Resilience4j` logic and monitors failure percentages out of a `slidingWindowSize` of calls. The criteria for an 'error' is driven by the `statusShouldOpenBreaker` matching expressions.

**Java context:**
When `ProxyClient` catches an exception, `ClusterCircuitBreakerRegistry` determines if the generated HTTP error `5xx` should increment the failure threshold, or if `4xx` should be completely ignored (treated as client misuse, not upstream failure).

**Constraints:**
- There is a known typo in the codebase parameters: `isEnanbled()` instead of `isEnabled()`.
- When the state is `OPEN`, subsequent HTTP calls to the `ProductClient` short-circuit instantly until `waitDurationInOpenStateMs` expires, after which it enters `HALF_OPEN`.
