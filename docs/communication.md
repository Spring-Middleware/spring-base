# Service Communication

This document describes how **service-to-service communication** works in Spring Middleware.

Spring Middleware provides **declarative HTTP clients** on top of Spring WebClient, backed by the **Registry Service** and the unified error / tracing model.

---

## 1. Overview

Application services communicate with each other through **declarative clients** instead of manually configuring HTTP clients or hard-coding URLs.

Key building blocks:

- `@MiddlewareClient` – declares a client interface for a remote service.
- **Registry Service** – provides cluster and node information used for service discovery.
- **Request context propagation** – every call carries `X-Request-ID` and `X-Span-ID` headers.
- **Unified error model** – remote errors are mapped to structured error responses and exceptions.

At a high level, a call looks like this:

1. Your code invokes a method on a `@MiddlewareClient` interface.
2. Spring Middleware resolves the **target cluster** and available **nodes** from the Registry.
3. A node is selected according to the configured strategy.
4. An HTTP request is built with propagated context headers and sent via WebClient.
5. The response (success or error) is mapped back to your method signature.

---

## 2. Declarative clients with `@MiddlewareClient`

A **middleware client** is a Java interface annotated with `@MiddlewareClient`. Methods on the interface are usually annotated with Spring MVC mapping annotations such as `@GetMapping`, `@PostMapping`, etc.

### 2.1 Basic example

```java
@MiddlewareClient(service = "product")
public interface ProductsApi {

    @PostMapping("/api/v1/products/bulk")
    List<ProductDto> createProducts(ProductBulkCreateRequestDto request);

}
```

**What this means:**

- `service = "product"` identifies the **cluster** in the Registry.
- The interface describes the **HTTP contract** (path, HTTP method, payload, return type).
- Spring Middleware creates a **proxy implementation** that:
  - looks up the `product` cluster in the Registry,
  - selects a node endpoint,
  - performs the HTTP request via WebClient,
  - maps the response to `List<ProductDto>`.

### 2.2 Responsibilities of a middleware client

For each call, Spring Middleware handles:

- **Service discovery** – resolves the logical service name to one or more node endpoints via Registry.
- **Node selection** – picks a node according to the configured strategy (round-robin, random, etc.).
- **Load distribution** – balances calls across healthy nodes.
- **Context propagation** – forwards `X-Request-ID` and `X-Span-ID` headers.
- **Error mapping** – converts HTTP errors to structured error payloads and exceptions.
- **Retry / resilience strategies** – optional retries and resilience behaviors, depending on configuration.
- **Spread calls** (optional) – some operations may be configured to call **all nodes** in a cluster instead of just one.

The exact configuration options for retries, spread calls and timeouts are defined by the underlying implementation in the `app` and communication modules.

---

## 3. Call flow

This section describes what happens inside the framework when a `@MiddlewareClient` method is invoked.

### 3.1 High-level flow

1. **Interface proxy invocation**  
   Application code calls a method on a `@MiddlewareClient` interface.

2. **Cluster resolution**  
   The framework reads the `service` attribute from `@MiddlewareClient` and identifies the **cluster** (e.g., `product`).

3. **Registry lookup**  
   The Registry is queried for the current `RegistryEntry` of that cluster, which includes:
   - `clusterEndpoint`
   - `nodeEndpoints`
   - `publicEndpoint`

4. **Node selection**  
   A **node selection strategy** chooses one or more node endpoints from `nodeEndpoints`. For spread calls, multiple nodes may be selected.

5. **Request construction**  
   - The target URL is built from the selected node endpoint and the method mapping (e.g. `/api/v1/products/bulk`).
   - The HTTP method (GET, POST, etc.) is determined from the Spring mapping annotation.
   - Request headers are prepared, including propagated `X-Request-ID` and `X-Span-ID`.
   - The request body is serialized from the method arguments.

6. **Execution via WebClient**  
   Spring Middleware uses Spring WebClient (or equivalent HTTP client) to perform the call.

7. **Response handling**  
   - On success, the response body is deserialized into the method’s return type.
   - On failure, the HTTP response is mapped into the unified **error model** (see below) and may throw a `RemoteServerException` or related error.

### 3.2 Spread calls

For some operations, it can be useful to call **all nodes** of a cluster (for example, to invalidate caches or broadcast an operation).

Spring Middleware supports **spread calls** where the client performs the same request against each node endpoint registered for a cluster. The exact API for enabling spread calls is implementation-specific, but at a high level it:

- obtains all `nodeEndpoints` from the Registry,
- executes the request against each one,
- aggregates responses and errors according to a defined strategy.

When you design spread calls, make sure that the underlying operation is **idempotent** or safe to repeat.

---

## 4. Request context propagation

Spring Middleware carries two context identifiers across every request:

| Header         | Description                                      |
|----------------|--------------------------------------------------|
| `X-Request-ID` | End-to-end request identifier across services    |
| `X-Span-ID`    | Local span identifier inside each service       |

### 4.1 Generation and forwarding

- When an external request enters the system, Spring Middleware ensures there is a **request ID** and an initial **span ID**.
  - If the incoming request already has `X-Request-ID` / `X-Span-ID`, they are reused.
  - Otherwise, new identifiers are generated.
- When a service calls another service via a `@MiddlewareClient`:
  - the current `X-Request-ID` is always forwarded,
  - a new `X-Span-ID` is generated for the **outgoing span**, or the local span is reused depending on the implementation.

This creates a simple but effective tracing chain without requiring a full tracing stack.

### 4.2 Example propagation chain

```
Client Request
   │
   ▼
Service A
requestId = 4C7F...
spanId = A12F...

   │
   ▼
Service B
requestId = 4C7F...
spanId = B992...

   │
   ▼
Service C
requestId = 4C7F...
spanId = C11A...
```

Each hop preserves the `requestId` and creates or updates a `spanId`, enabling log correlation and debugging.

For more details on how context appears in error responses, see `docs/errors.md`.

---

## 5. Error handling and propagation

Every remote call through a middleware client uses the **unified error model** of Spring Middleware.

### 5.1 Structured error responses

When a remote service returns an error, it is converted into a structured payload similar to:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {
    "requestId": "F4D29AAFE7FC4844A1FF8794F186B102",
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

Core classes used in the error model include:

- `ServiceException`
- `ErrorDescriptor`
- `ErrorMessage`
- `ErrorMessageFactory`
- `RemoteServerException`
- global `@RestControllerAdvice`

### 5.2 Remote errors and `RemoteServerException`

When a `@MiddlewareClient` call fails due to a remote error, the framework may throw a `RemoteServerException` (or a similar exception) that wraps:

- HTTP status code and message
- error code (e.g., `PRODUCT:NOT_FOUND`)
- human-readable message
- metadata such as:
  - `remote.url`
  - `remote.method`
  - `remote.service`
  - `requestId`
  - `spanId`
  - `callChain` (sequence of services involved)

Your application code can catch these exceptions and:

- inspect the error code / metadata,
- translate them into domain-specific responses,
- or rethrow them as-is to propagate the structured error.

### 5.3 Relationship with GraphQL

For GraphQL endpoints, Spring Middleware uses centralized exception handling (e.g., `GraphQLValidationExceptionHandler`) to map the same error model into GraphQL error responses with `extensions.code` and other metadata.

Service-to-service communication involving GraphQL endpoints still benefits from:

- the same `X-Request-ID` / `X-Span-ID` propagation,
- the same structured error semantics.

See `docs/errors.md` and `docs/graphql.md` for more details.

---

## 6. Configuration overview

The exact configuration options for clients depend on the concrete modules (primarily the `app` module and any communication-specific configuration classes). Typical configuration aspects include:

- **Registry endpoint** – where the service finds the Registry Service (usually via Spring Boot properties or environment variables).
- **Timeouts** – connection and read timeouts for HTTP clients.
- **Retry behavior** – number of retries, backoff strategy for idempotent operations.
- **Load-balancing / node selection strategy** – how nodes are picked for each call.
- **Spread call behavior** – whether and how to aggregate responses from multiple nodes.

Configuration is usually done via **Spring Boot configuration properties** and/or auto-configuration classes provided by Spring Middleware.

Refer to the `app` module documentation and code for concrete property names and defaults.

---

## 7. Related documentation

- `README.md` – high-level overview and examples of `@MiddlewareClient`.
- `docs/architecture.md` – overall architecture and control/data plane.
- `docs/errors.md` – detailed error model and error propagation.
- `docs/registry.md` – Registry model (`RegistryEntry`, `SchemaLocation`) and flows.
- `docs/graphql.md` – GraphQL support and centralized error handling.
