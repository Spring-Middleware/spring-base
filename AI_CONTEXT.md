PROJECT CONTEXT

Project: Spring-Middleware

Framework for Java microservices.

Stack
- Java 21
- Spring Boot 3
- WebClient
- Redis (Redisson)
- MongoDB
- GraphQL
- Maven
- Docker / Kubernetes

Architecture

There is a central Registry service.

Each microservice:
- registers REST resources with @Register
- registers GraphQL schemas
- reports node instances
- runs a consistency scheduler
- re-registers automatically if something is missing

Registry responsibilities:
- maintain service topology
- health check nodes
- remove dead nodes
- keep API metadata
- keep GraphQL schema locations

Concepts

cluster → logical service name (Kubernetes service)
node → pod instance (IP)

RegistryEntry
- clusterEndpoint
- nodeEndpoints
- publicEndpoint

SchemaLocation
- namespace
- location
- contextPath
- pathApi
- schemaLocationNodes

Framework capabilities
- service discovery
- API metadata registry
- GraphQL schema registry
- node lifecycle management
- annotation-driven clients (@MiddlewareClient)
- spread calls (call all nodes)
- self-healing registry

Error model
- ServiceException
- ErrorMessage
- ErrorMessageFactory
- @RestControllerAdvice


## Current Work (2026-03-05)

### GraphQL Error Handling

Implemented centralized GraphQL exception handling in `spring-base`.

Main class:
- `GraphQLValidationExceptionHandler`

Supported exception mapping:
- `ServiceException` / `ErrorDescriptor`
- `PersistenceException` with Hibernate constraint resolution
- `ConstraintViolationException` (Jakarta Validation)
- `LazyInitializationException` ignored
- Fallback to `FrameworkErrorCodes.UNKNOWN_ERROR`

GraphQL errors now follow a consistent structure:

Example:

```json
{
  "message": "Product with 9c537be6-0684-4744-8d9c-9144dde40fb4 not found",
  "path": ["product"],
  "extensions": {
    "code": "PRODUCT:NOT_FOUND"
  }
}