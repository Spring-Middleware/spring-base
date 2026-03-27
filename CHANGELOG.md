# Changelog

# Changelog

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