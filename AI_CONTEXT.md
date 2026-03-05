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