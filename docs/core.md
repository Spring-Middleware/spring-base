# Core Module (api, app, commons, view)

This document describes the "core" of Spring Middleware: the set of modules that provide fundamental APIs, application utilities and shared libraries used across the platform. The core area includes four main modules:

- `api` — API and annotation contracts used across modules and client/server integrations.
- `app` — application-level utilities, filters, controllers and boot helpers for services.
- `commons` — shared utilities, exceptions, converters and common infrastructure code.
- `view` — view transformation and role-based view helpers.

This document summarizes responsibilities, key packages/classes, and common configuration points so maintainers and users quickly understand what lives in the core and where to look.


## High level responsibilities

- Provide common annotations and contracts used by the rest of the platform (middleware contract, security annotations, GraphQL link annotations).
- Expose application infrastructure helpers: request logging, error handling, register-on-start, client wiring, and common filters.
- Provide shared utilities and low-level helpers (validation constraints, converters, exception types, environment helpers) to reduce duplication across modules.
- Offer the view transformation subsystem which enables role-based response shaping and view adaptors.


## Design and tone

The core modules are intentionally small and focused. Their public API should be stable and annotation-driven: prefer adding a small annotation and a focused helper class rather than exposing a large mutable runtime API. For runtime wiring and Spring-managed beans use `app`.


## `api` module

Purpose
: API contracts and small annotation primitives. This module contains stable annotations and simple helper interfaces that other modules and user services depend on.

Key packages & classes
- `io.github.spring.middleware.annotation` — core annotations used by the platform. Notable items:
  - `@MiddlewareContract` / `MiddlewareContractConnection` — metadata to declare middleware client configuration (timeouts, circuit-breaker parameters, connection limits).
  - `@Register` / `@RegisterSchema` — markers used by the registry integration and auto registration mechanisms.
  - `@MiddlewareCircuitBreaker` — circuit breaker configuration annotation used on middleware contracts.
  - GraphQL-related annotations under `annotation.graphql`: `@GraphQLLink`, `@GraphQLLinkArgument`, `@GraphQLLinkClass`, `@GraphQLType`, `@GraphQLCustomScalar`.
  - Security-related annotations under `annotation.security`: `@MiddlewareApiKey`, `@MiddlewarePassthrough`, `@MiddlewareClientCredentials`, `@MiddlewareRequiredScopes`.

- `io.github.spring.middleware.sort` — small sorting abstractions used by modules that expose search APIs.

Who uses it
- Mostly other core modules, the graphql and gateway modules, and user services that declare contracts.


## `app` module

Purpose
: Application-level utilities and common Spring components used by services. The `app` module contains the majority of runtime helpers used by bootable services.

Key packages & responsibilities
- `annotations` — helper annotations applied on components.
- `client` — helper types for declarative middleware client wiring and resilience (see `client.proxy`, `client.config` packages).
- `component` & `provider` — component implementations and provider interfaces used across services.
- `config` — Spring Boot auto-configuration classes and conditional wiring.
- `controller` & `error` — base controllers and error handling utilities that make error propagation uniform across services.
- `event`, `scheduler` — small scheduling/event helpers.
- `filter` — web filters, including `RequestLoggingFilter` (request/response logging) and context propagation.
- `graphql` — GraphQL helpers and integration points.
- `jms` — JMS helpers and resource handling used by RabbitMQ support.
- `log` — `LogRequestResponse` and helpers that control forced logging via `middleware.log.apiKey`.
- `manager` — lifecycle and managers (for registry, health, etc.).
- `mapper` — mapping helpers used across modules.
- `mock` — test/helpers used in unit/integration tests.
- `register` — bootstrap registration components used to register services/resources with the central registry.
- `scope` — request & service scoping helpers.
- `security` — security analyzers and appliers used for declarative clients and proxy wiring.

Key runtime classes (examples)
- `io.github.spring.middleware.client.RegistryClient` — example middleware client interface annotated with `@MiddlewareContract`.
- `io.github.spring.middleware.client.proxy.ProxyClientAnalyzer` — analyzes interfaces and configures proxy clients at startup.
- `io.github.spring.middleware.client.proxy.ClusterCircuitBreakerRegistry` / `ClusterBulkheadRegistry` — registries that manage client resilience objects.
- `io.github.spring.middleware.filter.RequestLoggingFilter` — HTTP filter that logs request & response payloads and supports forced logging using the configured API key.
- `io.github.spring.middleware.log.LogRequestResponse` — helper that decides whether to log even with insufficient log level (forced logging).

Notes
- `app` is the module that contains most of the runtime wiring. It is the right place to add components that are expected to be available in boot modules.


## `commons` module

Purpose
: Small, reusable utilities used across modules: converters, environment helpers, exception and error model classes, configuration helpers and general-purpose utilities.

Key packages & classes
- `config` — property binding and small config helpers (configuration properties classes used by other modules).
- `constraint` — custom validation constraints.
- `controller` — small controller helpers used by other modules.
- `converter` — general converters used in mapping and request processing.
- `data` — shared DTOs and data helpers.
- `environment` — helpers to read and adapt environment properties consistently.
- `error` & `exception` — the unified error model and exception utilities used by the framework.
- `resolver` — small resolution helpers (for example for binding names/values from environment or registry).
- `utils` — general utilities that don't otherwise belong anywhere else.

Why this module
- Keeps duplication low by centralizing small helpers that many modules need.


## `view` module

Purpose
: View transformation helpers used to convert internal domain objects to API representations with role-based shaping and view adaptors.

Key parts
- `View` interface and supporting adaptors that can produce different JSON content depending on roles and request context.
- `PropertyRolesAllowedAuthorizer` — role based authorizer used to include/exclude properties in view transformations.
- `DataAdaptor` — adaptation contract.
- `ContextFilterPredicate` — predicate helpers for contextual filtering of view properties.

When to use
- Use `view` to centralize view shaping logic (e.g., exposing different fields to different roles) and to keep controllers thin.


## Configuration examples (properties)

Below are common properties and suggested defaults used by core modules. Place these in `application.yml` (or environment variables) in services using Spring Middleware.

1) Request/response logging (`middleware.log`)

```yaml
middleware:
  log:
    apiKey: ${MIDDLEWARE_LOG_API_KEY:}
    request:
      enabled: true
    response:
      enabled: true
    responseTime:
      enabled: false
    exclude:
      urlPatterns:
        - "/actuator/**"
        - "/health/**"
```

Meaning:
- `middleware.log.apiKey` — optional key to force request/response logging when a request includes header `X-Logging-Key` with the same value.
- `middleware.log.request.enabled` — enable request bodies logging.
- `middleware.log.response.enabled` — enable response bodies logging.
- `middleware.log.responseTime.enabled` — enable response time logging.
- `middleware.log.exclude.urlPatterns` — ant-style patterns to exclude from logging.

Usage (forced logging):
- Send a request with header `X-Logging-Key: <apiKey>` to force the filter to emit logs even if logger level is not INFO. The `LogRequestResponse` helper will call logger at `error` level if INFO is disabled but the key matches.

2) Middleware client defaults and resilience configuration

```yaml
middleware:
  client:
    registry-endpoint: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
    defaultMaxConcurrent: 25
    product:
      connection:
        timeout: 30000
        max-connections: 50
        max-concurrent-calls: 200
        max-retries: 3
        retry-backoff-millis: 1000
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state-ms: 10000
        sliding-window-size: 20
        minimum-number-of-calls: 10
        permitted-number-of-calls-in-half-open-state: 3
        status-should-open-breaker: 5xx
        status-should-ignore-breaker: 4xx
      security:
        type: API_KEY # Options: NONE, PASSTHROUGH, API_KEY, OAUTH2_CLIENT_CREDENTIALS
        api-key: ${API_KEY_PRODUCT_SERVICE:default-product-api-key}
        oauth2:
          client-id: ${OAUTH2_CLIENT_ID_PRODUCT_SERVICE:product-service}
          client-secret: ${OAUTH2_CLIENT_SECRET_PRODUCT_SERVICE:}
          token-uri: ${OAUTH2_TOKEN_URI_PRODUCT_SERVICE:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/token}
```

Meaning and mapping
- `middleware.client.<name>.connection.*` map to `@MiddlewareContract.connection()` values.
- `middleware.client.<name>.circuit-breaker.*` map to `@MiddlewareContract.circuitBreaker()` values and are used by `ClusterCircuitBreakerRegistry` to configure circuit-breakers.
- `middleware.client.<name>.security.*` is used by the proxy client security appliers to set API key or oauth2 client credentials per proxy client.

Quick `@MiddlewareContract` example

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
  ),
  security = "${middleware.client.product.security.type:API_KEY}"
)
public interface ProductApiClient {
    @GetMapping("/api/v1/products/{id}")
    ProductDto getProduct(@PathVariable("id") UUID id);
}
```


## Examples and common patterns

1. Middleware clients (contract + resilience):
   - Declare `@MiddlewareContract` on an interface in your service or client module. Use the `app` wiring (client package) and `api` annotations to declare timeouts and circuit breaker parameters.

2. Request logging & forced logging:
   - The `app.filter` package provides `RequestLoggingFilter` which uses properties in `commons` to drive behaviour. The ability to force request logs with a header (X-Logging-Key) is provided by the logging helpers.

3. Error model and propagation:
   - Use the `commons.error` abstraction to normalize errors across modules. The `app.error` package integrates the error model with HTTP handling.


## Where to look in the code

- `parent/api/src/main/java/io/github/spring/middleware/annotation` — annotations and small contracts.
- `parent/app/src/main/java/io/github/spring/middleware` — application wiring and filters.
- `parent/commons/src/main/java/io/github/spring/middleware` — shared utilities.
- `parent/view/api/src/main/java/io/github/spring/middleware/view` — view and authorization helpers.


## Contributing

If you add a helper that is shared between modules, consider placing it in `commons` and keeping the API module annotation-only (stable surface) to reduce coupling between runtime helpers.

---

## Related documentation

- [README.md](../README.md)
- [Getting Started](./getting-started.md)
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [GraphQL](./graphql.md)
- [Kafka](./kafka.md)
- [RabbitMQ](./rabbitmq.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [JPA](./jpa.md)
- [Logging](./logging.md)
- [Security](./security.md)
- [Client Security](./client-security.md)
- [Client Resilience](./client-resilience.md)
- [Cache](./cache.md)
