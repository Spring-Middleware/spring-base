# AI_CONTEXT — Spring Middleware

## Project

Spring Middleware is a modular Java framework designed to build distributed microservice platforms with consistent infrastructure patterns.

The framework provides infrastructure components for:

- service discovery
- declarative service communication
- GraphQL federation
- structured error propagation
- distributed request tracing
- infrastructure integrations (Redis, MongoDB, RabbitMQ)
- security configuration
- registry-driven service topology

The framework is distributed via Maven Central using the BOM:

`io.github.spring-middleware:bom`

Example dependency:

```xml
<dependency>
  <groupId>io.github.spring-middleware</groupId>
  <artifactId>app</artifactId>
</dependency>
```

---

## Tech Stack

- Java 21 (preview features enabled)
- Spring Boot 3.4.x
- Spring Security
- WebClient
- Redis (Redisson)
- MongoDB
- GraphQL
- RabbitMQ
- Maven
- Docker
- Kubernetes

---

## Architecture

Spring Middleware uses a **registry-driven microservice architecture**.

A central **Registry Service** acts as the **control plane** of the platform.

The registry stores:

- service topology
- REST resource metadata
- GraphQL schemas
- active node instances
- cluster endpoints
- node endpoints

Each microservice automatically:

- registers REST resources
- registers GraphQL schemas
- reports running node instances
- runs a topology consistency scheduler
- re-registers resources if missing

Registration is annotation-driven.

Example:

```java
@Register
@RestController
```

---

## Core Concepts

### cluster

Logical service identifier.

Typically corresponds to a Kubernetes Service or logical microservice name.

Examples:

- product-service
- catalog-service

### node

Running instance of a cluster.

Examples:

- Kubernetes pod
- Docker container
- VM instance
- container IP endpoint

---

## Middleware Contracts

Services expose remote contracts using `@MiddlewareContract`.

Example:

```java
@MiddlewareContract(name = "product")
public interface ProductsApi {
  Product getProduct(String id);
}
```

Other services consume them using declarative clients.

Example:

```java
@MiddlewareClient(service = "product")
ProductsApi productsApi;
```

Capabilities:

- registry based service discovery
- automatic endpoint resolution
- WebClient configuration
- request context propagation
- structured remote error handling
- retry / resilience strategies
- optional spread calls (call all nodes)

---

## Request Context Propagation

Every request propagates two identifiers:

- `X-Request-ID`
- `X-Span-ID`

Purpose:

- correlate requests across services
- lightweight distributed tracing
- debugging and log correlation

Example chain:

```text
Client
  ↓
Service A (span A1)
  ↓
Service B (span B3)
  ↓
Service C (span C7)
```

---

## Security

Spring Middleware includes a configurable security module built on top of Spring Security.

Configuration prefix:

`middleware.security`

Supported security types:

- `NONE`
- `BASIC_AUTH`
- `JWT` (planned)
- `API_KEY` (planned)

Example configuration:

```yaml
middleware:
  security:
    type: BASIC_AUTH

    public-paths:
      - /api-docs/**
      - /swagger-ui/**

    protected-paths:
      - path: /api/v1/catalogs/**
        type: ROLES
        methods: [GET]
        allowed-roles: [GET_USER, ADMIN]

    basic:
      credentials:
        username: admin
        password: admin
        roles: [ADMIN]
```

Protected paths are evaluated dynamically during security configuration.

Authorization rules are translated into Spring Security `requestMatchers`.

### Authorization Model

Spring Middleware uses a unified authorization model across all authentication types.

Protected paths are declared under:

`middleware.security.protected-paths`

Each rule contains:

- `path`
- `type` (one of `NONE`, `AUTHENTICATED`, `ROLES`)
- `methods`
- `allowed-roles` (only used when `type = ROLES`)

Semantics:

- `NONE` – the rule marks the path as **public** (no authentication required).
- `AUTHENTICATED` – the path requires the user to be **authenticated**, but no specific role is enforced.
- `ROLES` – the path requires the user to be **authenticated** and to have at least one of the roles in `allowed-roles`.

Important behavior:

- rules are evaluated in declaration order
- the first matching rule wins
- more specific paths must appear before broader ones

### Protected Path Resolution

Protected paths are resolved internally through a dedicated resolver component.

Main component:

`ProtectedPathRuleResolver`

Responsibilities:

- evaluate rules in declaration order
- match HTTP method
- match path patterns
- return the first matching rule along with its `type` and `allowed-roles`

This resolver allows authentication filters (API Key, JWT, OIDC) to determine whether a request requires authentication
and, when `type = ROLES`, to verify role-based access.

### Authentication vs Authorization Responses

Spring Middleware distinguishes between authentication failures and authorization failures.

Authentication failures return **401 Unauthorized**.

Authorization failures return **403 Forbidden**.

Typical scenarios:

- missing credentials on a protected path that requires authentication → **401**
- invalid credentials → **401**
- valid credentials but insufficient roles on a `type = ROLES` path → **403**

Authentication failures are handled through a custom Spring Security `AuthenticationEntryPoint`.

### Security Error Integration

Security errors are integrated with the framework error handling infrastructure.

Authentication and authorization exceptions are resolved through the same error resolution pipeline used by the rest of the framework:

- `ErrorMessageFactory`
- `CompositeThrowableErrorResolver`
- `CompositeHttpStatusCodeResolver`

This guarantees that security failures produce the same structured error response format as application errors.

### Mandatory Public Endpoints

Spring Middleware automatically exposes a small set of **framework-level health endpoints** that are always accessible without authentication.

These endpoints are required for internal platform operations such as **registry node health checks** and **cluster topology maintenance**.

The framework guarantees that the following endpoints are always publicly accessible regardless of security configuration:

```text
/{contextPath}/_alive
/{contextPath}/graphql/_alive
```

Examples:

```text
/product/_alive
/product/graphql/_alive

/catalog/_alive
/catalog/graphql/_alive

/registry/_alive
```

These endpoints are added automatically by the framework through an internal list of **mandatory public paths** and do not require explicit configuration under `middleware.security.public-paths`.

This mechanism ensures that:

- the **registry service can perform node liveness checks**
- internal cluster infrastructure is not accidentally blocked by user-defined security rules
- health probes remain stable across different authentication configurations

---

## Error Model

Errors are propagated using a structured error model.

Core classes:

- `ServiceException`
- `ErrorDescriptor`
- `ErrorMessage`
- `ErrorMessageFactory`
- `RemoteServerException`
- `@RestControllerAdvice`

Example error payload:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {}
}
```

Remote errors preserve metadata:

- `remote.url`
- `remote.method`
- `remote.service`
- `requestId`
- `spanId`
- `callChain`

---

## GraphQL Support

GraphQL infrastructure is registry-driven and supports distributed schema federation.

Capabilities:

- schema registration
- namespace management
- centralized GraphQL error handling
- schema federation foundation

Example GraphQL error:

```json
{
  "message": "Product not found",
  "path": ["product"],
  "extensions": {
    "code": "PRODUCT:NOT_FOUND"
  }
}
```

---

## Messaging Infrastructure (RabbitMQ)

Spring Middleware provides a **JMS-style abstraction layer on top of RabbitMQ**.

The messaging module supports:

- annotation-driven producers and consumers
- automatic destination creation
- exchange and binding management
- durable and transient queues
- topic-based event propagation
- distributed event delivery across cluster nodes

Messaging resources are defined through annotations such as:

```java
@JmsDestination
@JmsProducer
@JmsConsumer
```

The framework automatically:

- resolves exchange names
- creates bindings
- configures queue arguments
- manages consumer lifecycle

Queues may include RabbitMQ arguments such as:

- `x-expires` (auto-delete inactive queues)
- `durable`
- `autoDelete`

This allows **ephemeral node-level messaging infrastructure** that automatically cleans up when a node leaves the cluster.

---

## Cluster Event Propagation

Cluster nodes synchronize their state through **RabbitMQ topic exchanges**.

Each node creates its own **node-specific queue** and binds it to the cluster event exchange.

Example queue name pattern:

```text
client-events-{cluster}-{nodeId}
```

Queues typically use expiration arguments such as:

```text
x-expires = 60000
```

This ensures that:

- node queues disappear automatically when a node stops
- no manual cleanup is required
- cluster messaging remains consistent

Events propagated across the cluster include:

- client configuration refresh
- service availability changes
- registry updates
- topology refresh signals

Example event flow:

```text
Node A
  │
  │ publish event
  ▼
RabbitMQ Topic Exchange
  │
  ├── Node B queue
  ├── Node C queue
  └── Node D queue
```

This enables **distributed coordination without direct node-to-node communication**.

---

## Registry Endpoint Model

The registry maintains several endpoint types to describe the topology of the platform.

### clusterEndpoint

Represents the **base endpoint of a service cluster**.

Example:

```text
product:8080/product
```

### resourceEndpoint

Represents the **fully qualified endpoint of a registered resource**.

Example:

```text
product:8080/product/graphql
product:8080/product/api
```

### nodeEndpoint

Represents the **endpoint of a specific running node instance**.

Example:

```text
172.21.0.5:8080/product/graphql
```

### publicEndpoint

Optional externally accessible endpoint used when services are exposed through
an external gateway or ingress.

Example:

```text
api.example.com/product/graphql
```

### Endpoint Composition

Endpoints are constructed using the following elements:

- `cluster`
- `node`
- `port`
- `contextPath`
- `resourcePath`

---

## Modules

Spring Middleware is composed of multiple modules organized as a multi-module Maven repository.

### Core

- commons
- api
- app
- model
- view

### Data

- mongo
- jpa
- redis
- cache

### Messaging

- rabbitmq

### Platform

- registry
- graphql

---

## Repository Layout

Infrastructure modules typically follow this structure:

```text
module
 ├─ api
 ├─ core
 └─ core-react (optional)
```

Typical dependency hierarchy:

```text
boot
 ↓
core
 ↓
api
```

Infrastructure integration:

```text
service
 ↓
middleware app
 ↓
middleware infrastructure modules
```

---

## Recent Improvements (2026-03)

GraphQL centralized exception handling.

Main component:

```text
GraphQLValidationExceptionHandler
```

Security improvements:

- unified authorization model across authentication types
- introduction of `ProtectedPathRuleResolver`
- API Key authentication infrastructure
- integration of Spring Security exceptions with `ErrorMessageFactory`

Cluster improvements:

- RabbitMQ-based cluster event propagation
- node-scoped event queues with automatic expiration
- asynchronous client reconfiguration signals across nodes
- distributed registry consistency through event messaging
- mandatory framework-level liveness endpoints for node health verification

---

## Current Status

Version: **1.1.0**  
Java: **21**  
Spring Boot: **3.4.x**

Status: **Active development**

Current capabilities:

- registry-driven microservices
- declarative service clients
- service topology management
- request/span context propagation
- structured error propagation
- Redis / Mongo / JPA infrastructure
- GraphQL schema registry
- centralized error handling
- configurable security module
- BOM distribution via Maven Central
- RabbitMQ-based cluster messaging

---

# Context Maintenance Rules

When the user asks to **add something to the context** ("añadir al contexto"), the following rules apply:

- The assistant must return the **entire context document**, not only the added section.
- Existing sections **must not be modified, reordered, or removed** unless explicitly requested.
- New information should be **appended in the most relevant section** or added as a new subsection.
- The goal is to **extend the context while preserving stability of the document structure**.
- The resulting document must remain **fully copy-paste safe**.

Additional clarification:

- The assistant **must not rewrite existing explanations**, even if they could be improved.
- The assistant **must not refactor section names or headings** unless explicitly requested.
- The assistant **must not collapse or summarize sections** of the document.
- The assistant **must treat this document as a stable knowledge base**, extending it incrementally instead of regenerating it.

---

# Documentation Output Rules

When generating Markdown documentation for this project:

- Always return the document inside a fenced block using `~~~~markdown` instead of ` ```markdown `.
- This avoids breaking nested code blocks that contain triple backticks, for example XML, YAML, JSON, or Java examples.
- The content inside the block must be valid Markdown and safe to copy directly into `.md` files.
- This rule applies whenever documentation is requested for:
  - README files
  - architecture documentation
  - AI_CONTEXT updates
  - examples or guides
  - any `.md` content
~~~~markdown

---

