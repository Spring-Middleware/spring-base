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

## OAuth2 / OIDC Service Authentication

Spring Middleware supports **service-to-service authentication using OAuth2 Client Credentials**.

This mechanism allows one microservice to securely call another using a **machine-to-machine access token** issued by an OIDC provider such as **Keycloak**.

Client configuration is declared directly on contract interfaces.

Example:

```java
@MiddlewareClientCredentials(
        tokenUri = "${middleware.client.product.oauth.token-uri}",
        clientId = "${middleware.client.product.oauth.client-id}",
        clientSecret = "${middleware.client.product.oauth.client-secret}"
)
```

Capabilities:

- OAuth2 **client_credentials grant**
- automatic token acquisition
- automatic token caching
- token propagation through `Authorization: Bearer` headers
- integration with framework error handling
- configurable scopes

Example token request:

```text
POST /realms/spring-middleware/protocol/openid-connect/token
grant_type=client_credentials
scope=product.read
```

---

## Security Client Types

Spring Middleware proxy clients support multiple authentication strategies.

These strategies are configured declaratively through annotations on contract interfaces.

Supported strategies:

### CLIENT_CREDENTIALS

Uses OAuth2 **client_credentials** grant to obtain a token from the identity provider.

Typical use case:

- service-to-service authentication

Token flow:

```text
Service A
  │
  │ request token
  ▼
OIDC Provider (Keycloak)
  │
  │ access_token
  ▼
Service A → Service B
Authorization: Bearer <token>
```

### API_KEY

Adds a static API key to outbound requests.

Example:

```java
@MiddlewareApiKey(
        headerName = "X-API-KEY",
        value = "${middleware.client.product.security.api-key}"
)
```

Typical use cases:

- internal services
- gateway-to-service authentication
- legacy system integration

### PASSTHROUGH

Forwards the inbound authentication header to downstream services.

Example:

```java
@MiddlewarePassthrough(
        headerName = "Authorization"
)
```

Typical use cases:

- gateway → service propagation
- user authentication delegation
- chained service calls

Passthrough enables **end-user identity propagation across service boundaries**.

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

### GraphQL Federation Gateway

Spring Middleware provides a **distributed GraphQL federation gateway** that dynamically composes schemas registered by services in the registry.

Main responsibilities:

- collect GraphQL schemas from registered services
- merge schemas into a unified gateway schema
- resolve remote fields through service contracts
- route GraphQL queries to the correct downstream service
- normalize responses and scalar types

The gateway supports:

- remote field resolution
- cross-service GraphQL queries
- scalar normalization
- centralized error propagation

### Query Execution Model

Incoming GraphQL queries are analyzed by the gateway and translated into **downstream GraphQL queries** targeting the owning service.

Execution flow:

```text
Client
  │
  ▼
GraphQL Gateway
  │
  ├── query analysis
  ├── field ownership resolution
  └── downstream query execution
        │
        ▼
  product-service / catalog-service / other services
```

The gateway reconstructs the final response by combining downstream responses into a unified result.

### Inline Fragment Support

The federation gateway supports **GraphQL inline fragments** when building downstream queries.

Inline fragments are required when querying polymorphic types or interfaces across services.

Example:

```graphql
{
    product(id: "123") {
        id
        ... on PhysicalProduct {
            stockQuantity
        }
        ... on DigitalProduct {
            downloadUrl
        }
    }
}
```

The query builder preserves fragment structures when generating downstream service queries.

### GraphQL Link Resolution Model (New)

Spring Middleware introduces a **declarative GraphQL link model** to resolve fields across services.

Fields annotated with `@GraphQLLink` represent **remote field resolution boundaries**.

Example:

```java
@GraphQLLink(
    schema = "product",
    type = "Product",
    query = "productsByIds",
    arguments = {
        @GraphQLArgument(name = "ids")
    },
    collection = true
)
private List<UUID> productIds;
```

Key characteristics:

- links are resolved dynamically at runtime
- the gateway determines the target schema and operation
- arguments are extracted from the source object
- remote GraphQL queries are constructed automatically
- responses are normalized and merged into the parent result

### GraphQL Link Argument Transport

GraphQL links support **multi-argument resolution** through a dedicated transport mechanism.

To support dynamic argument passing across services, Spring Middleware introduces:

```text
GraphQLLinkArguments (scalar)
```

This scalar acts as an **internal transport container** for argument maps.

Design principles:

- hides `Map<String,Object>` from the GraphQL schema
- avoids SPQR object mapping issues
- allows leaf-level GraphQL fields (no subselection required)
- enables dynamic argument composition

Example:

```java
@GraphQLQuery(name = "productsByName")
@GraphQLLink(
    schema = "product",
    type = "Product",
    query = "products",
    arguments = {
        @GraphQLArgument(name = "q"),
        @GraphQLArgument(name = "catalogId")
    },
    collection = true
)
public GraphQLLinkArguments getProductsByName(@GraphQLArgument(name = "name") String name) {
    return new GraphQLLinkArguments(Map.of("q", name, "catalogId", id));
}
```

Execution behavior:

- the subgraph returns a scalar value (no subselection required)
- the gateway extracts the underlying map
- arguments are mapped to the target operation
- remote query is executed with constructed variables

### GraphQL Type Mapping Customization

Spring Middleware extends SPQR type mapping using a custom `TypeMapper`.

Purpose:

- force specific Java types to be treated as GraphQL scalars
- avoid incorrect object type generation
- integrate internal transport types into the schema generation pipeline

Example:

```java
GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator()
    .withTypeMappersPrepended(new GraphQLLinkArgumentsTypeMapper());
```

The custom mapper ensures:

- `GraphQLLinkArguments` is mapped to a scalar
- no object type is generated for internal transport classes
- schema generation remains stable
- runtime execution aligns with federation behavior

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

## Messaging Infrastructure (Kafka)

Spring Middleware provides a **declarative abstraction layer on top of Apache Kafka**.

The Kafka module is designed to follow the same platform-oriented principles as the rest of the framework:

- annotation-driven infrastructure
- centralized declarative configuration
- reusable transport abstractions
- distributed tracing integration
- consistent developer experience across infrastructure modules

Kafka configuration is declared under:

`spring.middleware.kafka`

Example configuration:

```yaml
spring:
  middleware:
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

      publishers:
        catalog-events:
          topic: ${KAFKA_TOPIC_CATALOG:catalog-events}

      subscribers:
        catalog-events:
          group-id: ${KAFKA_GROUP_ID_CATALOG:catalog-service-group}
          topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
          concurrency: 1
```

### Publisher Model

Kafka publishing is exposed through a **registry-driven publisher abstraction**.

Example usage:

```java
kafkaPublisherRegistry
        .getPublisher("catalog-events")
    .publish(event);
```

Publishing with key:

```java
kafkaPublisherRegistry
        .getPublisher("catalog-events")
    .publish(event, eventId);
```

The publisher abstraction provides:

- topic resolution through configuration
- keyed and non-keyed publishing
- event wrapping through a shared envelope
- integration with distributed trace identifiers
- centralized publisher lookup by logical name

### Event Envelope

Kafka events are wrapped in a standard transport envelope:

```java
EventEnvelope<T>
```

Main fields:

- `eventId`
- `eventType`
- `timestamp`
- `traceId`
- `payload`

This envelope provides:

- transport-level metadata
- event traceability
- separation between domain payload and infrastructure metadata
- consistency across event-driven components

### Subscriber Model

Kafka consumers are declared using annotations.

Example:

```java
@MiddlewareKafkaListener("catalog-events")
public void handleCatalogEvent(EventEnvelope<CatalogEvent> event) {
    log.info("Received catalog event: {}", event.getPayload());
}
```

Subscriber configuration resolves:

- topic
- group-id
- concurrency

This enables:

- listener declaration in code
- infrastructure configuration in YAML
- decoupling between logical subscriber name and physical topic

### Listener Registration

Kafka listeners are discovered at startup through a dedicated registrar.

The registrar is responsible for:

- scanning configured base packages
- identifying methods annotated with `@MiddlewareKafkaListener`
- resolving subscriber configuration
- dynamically registering listener containers

This model aligns Kafka listener discovery with other Spring Middleware infrastructure registration patterns.

### Topic Management

Kafka topics may be created in different ways depending on the environment.

Development mode may rely on broker auto-creation.

Production-oriented setups should explicitly define topics, including:

- partitions
- replicas
- retention-related policies

Future declarative topic model example:

```yaml
spring:
  middleware:
    kafka:
      topics:
        catalog-events:
          partitions: 3
          replicas: 1
```

### Partitioning Strategy

Kafka partitioning is a key part of the design model.

Principles:

- messages with the same key are routed to the same partition
- ordering is guaranteed within a partition
- throughput and parallelism scale through partitions

Example:

```java
publish(event, event.getId());
```

This allows:

- ordering per business entity
- scalable parallel consumption
- partition-aware event processing

### Kafka Runtime Behavior and Constraints

Kafka runtime behavior introduces several important constraints that directly impact system design:

- **parallelism is bounded by partitions**

```text
max_parallelism = number_of_partitions
```

- **consumer concurrency must not exceed partitions**

```text
concurrency <= partitions
```

Exceeding this does not increase throughput and may introduce unnecessary thread overhead.

- **multiple listeners with the same groupId compete for partitions**

This is not a clean scaling model:

```text
2 listeners + same groupId → competing consumers
```

Instead:

- use **one logical listener with concurrency** for parallelism
- use **different groupIds** when the same event must be processed by different independent flows

- **partition assignment is hash-based**

```text
partition = hash(key) % partitions
```

Implications:

- no guarantee of uniform distribution with a small key set
- collisions are expected
- distribution improves statistically with many distinct keys

- **key strategy defines scalability**

Guidelines:

- use **high-cardinality keys (UUID, eventId)** for throughput and distribution
- use **business keys (orderId, userId)** when strict ordering per entity is required
- avoid small fixed key sets (e.g., A/B/C) as they create partition hotspots

- **Kafka delivery semantics**

Kafka guarantees:

```text
at-least-once delivery
```

Implications:

- duplicate events are possible
- consumers must implement **idempotent processing**

Recommended approach:

- use `eventId` from `EventEnvelope`
- maintain a processed-event store (e.g., Redis, DB)
- ignore already processed events

- **offset and consumer stability**

Typical failure scenarios:

- missing persistence (no volumes)
- inconsistent offsets after restart
- frequent rebalances due to consumer topology changes

Common error:

```text
Timeout before position could be determined
```

Mitigation:

- stable cluster configuration
- persistent Kafka storage
- avoid unnecessary consumer group churn

- **Kafka as transport layer**

Kafka does not provide business guarantees:

```text
Kafka → transport
Application → consistency
```

Responsibility boundaries:

- Kafka ensures delivery and ordering per partition
- the application ensures correctness, idempotency, and state consistency

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
- kafka

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
- OAuth2 **client_credentials service authentication**
- OIDC resource server integration
- passthrough authentication propagation
- support for `issuer-uri` or `jwk-set-uri` validation
- integration of Spring Security exceptions with `ErrorMessageFactory`

Cluster improvements:

- RabbitMQ-based cluster event propagation
- node-scoped event queues with automatic expiration
- asynchronous client reconfiguration signals across nodes
- distributed registry consistency through event messaging
- mandatory framework-level liveness endpoints for node health verification

GraphQL improvements:

- distributed GraphQL federation gateway
- dynamic schema merging from registered services
- inline fragment support in query builder
- scalar normalization during response merging
- polymorphic GraphQL response handling across services
- declarative GraphQL link model for cross-service field resolution
- scalar-based argument transport for GraphQL links
- SPQR type mapping customization for internal transport types

Kafka improvements:

- declarative Kafka publisher/subscriber model
- registry-driven publisher resolution
- annotation-based listener registration
- event envelope standardization
- integration with distributed tracing
- foundation for partitioning and scalable event processing
- clarified runtime model for partitions, concurrency, and consumer groups
- explicit idempotency strategy using eventId
- improved partitioning strategy guidelines (high-cardinality keys vs business keys)
- documented consumer competition vs parallelism model

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
- OAuth2 service authentication
- BOM distribution via Maven Central
- RabbitMQ-based cluster messaging
- Kafka-based event streaming infrastructure
- GraphQL federation with dynamic link resolution
- scalar-based internal argument transport for GraphQL links

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

Additional clarification:

- The assistant **must never replace parts of the document with placeholders** such as:
    - `[...]`
    - `[... contenido ...]`
    - `[... SIN MODIFICAR ...]`
- The assistant must always return the **full explicit content**, even if sections are unchanged.
~~~~markdown