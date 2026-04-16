# Changelog

## 1.5.0

### Added

- Expanded GraphQL federation documentation with a dedicated public page for registry-driven schema composition, GraphQL Links, and distributed execution.
- New public demo/reference material for declarative GraphQL batching, including video walkthrough and article-level technical explanation.
- Additional documentation around batching internals, execution flow, instrumentation, and request-scoped dispatch behavior.

### Improved

- GraphQL documentation structure and messaging for federation, cross-service resolution, batching, error handling, and adoption path.
- Clarity around platform-level N+1 resolution, making the gateway batching model easier to understand and present.
- Overall project presentation for GraphQL capabilities across website and docs.

### Upgraded

- Platform BOM and project version bumped to 1.5.0.

## 1.4.0

### Added

- Declarative GraphQL Batching support for resolving N+1 distributed queries automatically via API Gateway.
- New `@GraphQLLink` settings (`batched`, `targetFieldName`) to declare grouped remote relationships without writing manual resolvers.
- Complete documentation for GraphQL Query Batching and Links (`docs/graphql-query-batching.md`).

### Upgraded

- Platform BOM and all base modules versions bumped to 1.4.0 (some inner modules to 1.5.0/1.7.0).

## 1.3.0

### Added

- Documentation updates and module version bump to 1.3.0 for platform BOM.
- GraphQL links documentation and gateway improvements (query building and selection set preservation).
- Kafka error handling configuration and documentation.

### Improved

- Documentation clarity and module docs linking.

### Upgraded

- BOM version set to 1.3.0 and core module versions aligned in docs.

## 1.2.0

### Added

- Service-side security configuration with role-based access rules
- Client-side security propagation for distributed calls
- Circuit breaker support for resilient service communication
- Dynamic GraphQL schema generation based on registry nodes
- GraphQL union type support with dynamic TypeResolver
- Custom GraphQL scalars (Instant, URI, UUID, BigDecimal, Long)
- Registry-driven messaging bootstrap and JMS lifecycle coordination
- Structured GraphQL error propagation with full remote metadata
- Support for `__typename`-based resolution in dynamic responses

### Improved

- ErrorMessage as unified error model across framework layers
- Remote error propagation without information loss (requestId, spanId, remote metadata)
- GraphQL exception handling pipeline (removed intermediate exception layer)
- TypeDefinitionRegistry merging logic for unions and schema composition
- Query builder for GraphQL inline fragments
- Messaging initialization order and startup synchronization

### Fixed

- Incorrect union type merging using stringified types instead of actual names
- Loss of fields in normalized GraphQL responses (`__typename`-only issue)
- Malformed GraphQL queries (duplicate `{` in inline fragments)
- Remote error fallback to `UNKNOWN_ERROR` losing original context
- RabbitMQ queue declaration conflicts (durable vs non-durable mismatch handling)

### Upgraded

- Internal GraphQL wiring and schema composition capabilities
- Resilience and fault-tolerance mechanisms across service calls

## 1.1.0

### Added

- Microservice registry runtime
- Declarative HTTP clients (@MiddlewareClient)
- Structured remote error propagation
- GraphQL validation exception handler
- Request context propagation (REQUEST_ID / SPAN_ID)

### Improved

- ErrorMessageFactory resolution pipeline
- RemoteServerException payload propagation
- ControllerAdvice error handling

### Upgraded

- Java 21 support
- Spring Boot 3.4.2