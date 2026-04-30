# Service Communication (RAG-Friendly)

## Quick Answer

**How do microservices communicate natively in Spring Middleware?**
They communicate using declarative HTTP clients (via `@MiddlewareClient`) backed by the Registry Service. There is no need to manually instantiate `WebClient` or hardcode target URLs.

**Java code (Declarative HTTP Client):**
```java
import io.github.spring.middleware.annotation.client.MiddlewareClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@MiddlewareClient(service = "product")
public interface ProductsApi {

    @PostMapping("/api/v1/products/bulk")
    List<ProductDto> createProducts(@RequestBody ProductBulkCreateRequestDto request);

}
```

**Constraints:**
- The `service` attribute value must exactly match the cluster ID defined inside the Registry.
- The interface automatically uses Spring MVC annotations (`@GetMapping`, `@PostMapping`).
- Load-balancing, service discovery, error propagation, and context header forwarding are baked seamlessly into the proxy implementation.

---

## Remote Error Handling

### What happens when an external declarative client call fails with an HTTP error?
If the remote service throws an HTTP error (e.g., `404 Not Found`), Spring Middleware converts it into a `RemoteServerException` structured symmetrically with the framework's centralized error model.

**JSON Payload mapping (Example HTTP 404 mapped into `RemoteServerException`):**
```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {
    "requestId": "F4D29AAFE7FC4844A1FF879...102",
    "span": [
      {
        "service": "catalog",
        "method": "replaceProducts",
        "httpStatus": 404
      }
    ]
  }
}
```

**Constraints:**
- A `RemoteServerException` inherently wraps the domain error code (e.g., `PRODUCT:NOT_FOUND`) and the entire call chain metadata.
- Using `@RestControllerAdvice` globally bubbles these standardized structured payloads to the end-user API.

---

## Distributed Tracing Context

### Does the declarative client propagate tracing context to downstream services automatically?
Yes. Every request generated automatically passes two exact header keys identifying the trace context across boundaries.

**Traced Headers:**
- `X-Request-ID`: Represents the single entry-point trace identity spanning multiple microservice hops.
- `X-Span-ID`: Representing an individual step in the trace that changes per hop.

**Constraints:**
- If the current HTTP thread doesn't have an `X-Request-ID` or `X-Span-ID`, a new one is generated before handing off the request to `WebClient`.
- This simple identifier propagation takes place without a heavy OpenTelemetry infrastructure stack.

---

## Calling All Nodes in a Cluster

### How do I trigger an API request universally across all instances of a service?
Spring Middleware supports **spread calls** for idempotent operations.

**Configuration logic:**
When enabled for a method mapping, the internal WebClient requests the given path mapping not to just one Registry `clusterEndpoint` randomly, but iteratively to each `nodeEndpoint` registered within that cluster.
This requires careful endpoint design as the downstream endpoint *must* safely tolerate identical repeated calls or handle global state resets (e.g., flushing internal cache registries).
