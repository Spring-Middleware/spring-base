# GraphQL Support

This document describes the **GraphQL infrastructure provided by Spring Middleware** and the direction of the platform regarding **distributed GraphQL federation**.

Spring Middleware treats GraphQL as a **platform capability in a distributed microservice architecture**, where:

- each service owns its **own GraphQL schema**
- schemas are **registered in the platform registry**
- a gateway can **discover and compose these schemas dynamically**

The goal is to enable **federated GraphQL APIs across autonomous microservices** without tightly coupling services together.

---

# 1. Current capabilities

Spring Middleware currently provides the **foundational infrastructure required for GraphQL federation**, focusing on:

- schema discovery
- schema metadata registration
- centralized error handling
- platform-level consistency
- a **GraphQL gateway service** that can load and expose registered schemas

At this stage, Spring Middleware focuses on **schema registration and discovery** and provides a **reference GraphQL gateway implementation** instead of a full-blown feature-complete federation engine.

The `graphql-gateway` module is meant as a **platform-native gateway** that consumes the Registry and exposes a single GraphQL endpoint.

---

# 1.1 Schema location registration

When a service exposes a GraphQL schema, Spring Middleware can register **where that schema lives** so that other platform components can discover it later.

The registry keeps track of:

- the **namespace** of each schema
- the **location** of the schema file or endpoint
- the **HTTP context path** and API path
- the **nodes** that serve that schema

This information is represented by the `SchemaLocation` model.

In practice this means:

- each service that participates in GraphQL can register its schema locations in the Registry
- the **GraphQL gateway** can later read this information and decide how to compose or federate schemas

Spring Middleware itself does **not currently**:

- perform advanced schema stitching or query planning across services
- execute complex GraphQL federation with per-field ownership rules

Instead, it focuses on providing **reliable metadata about where schemas are and which services own them**, plus a **simple gateway implementation** that can execute queries against the composed schema.

This makes it possible for external components (for example a more advanced GraphQL gateway) or the built-in `graphql-gateway` service to dynamically compose APIs across services.

---

# 1.2 SchemaLocation model

The `SchemaLocation` model describes GraphQL schema metadata stored in the Registry.

Key fields include:

| Field | Description |
|------|-------------|
| `namespace` | Logical domain or namespace of the schema |
| `location` | Location of the schema definition (classpath, file, URL) |
| `contextPath` | HTTP context path where the service is exposed |
| `pathApi` | Specific HTTP path of the GraphQL endpoint |
| `schemaLocationNodes` | Nodes currently exposing the schema |

Services typically do not interact directly with `SchemaLocation`; instead, they rely on Spring Middleware’s **automatic registry integration** to publish schema metadata.

---

# 2. Distributed GraphQL architecture

Spring Middleware is designed for **distributed GraphQL architectures**, where each microservice exposes its own schema.

Example topology:

```
                    ┌─────────────────────┐
                    │   GraphQL Gateway   │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼

 ┌───────────────┐      ┌───────────────┐      ┌───────────────┐
 │ product-service│      │ catalog-service│     │ inventory-service│
 │               │      │               │      │               │
 │ GraphQL API   │      │ GraphQL API   │      │ GraphQL API   │
 └───────────────┘      └───────────────┘      └───────────────┘
```

Each service owns:

- its GraphQL schema
- its resolvers
- its data access logic

The gateway composes these schemas into a **single API exposed to clients**.

---

# 2.1 GraphQL Gateway service

Spring Middleware includes a **GraphQL gateway service** under the `parent/graphql-gateway` modules.

The gateway:

- discovers registered `SchemaLocation` entries from the Registry
- loads the schemas referenced by those locations
- builds a GraphQL executable schema
- exposes a single `/graphql` HTTP endpoint

Clients interact only with the gateway and do not talk directly to individual service schemas.

For more details, see [parent/graphql-gateway/README.md](parent/graphql-gateway/README.md).

---

# 3. Future federation and composition

Spring Middleware is designed so that **another service** can consume the registered schema locations and build a unified GraphQL API.

A typical setup looks like this:

1. Each microservice registers its GraphQL schema location in the Registry.
2. The **GraphQL gateway service** reads all `SchemaLocation` entries from the Registry.
3. The gateway loads all schemas and **composes them into a single schema** (today this is a simple composition without advanced query planning).
4. Client queries are executed through this gateway.

Execution flow:

```
Client
  │
  ▼
GraphQL Gateway
  │
  ├── analyze query
  ├── resolve fields against composed schema
  └── execute downstream calls (depending on implementation)
        │
        ▼
     target services
```

This allows:

- cross-service GraphQL queries
- a single GraphQL endpoint for clients
- independent evolution of service schemas

In this model:

- Spring Middleware is responsible for **schema metadata and discovery**
- the gateway is responsible for **composition and query execution**

---

# 4. Inline fragments and polymorphic types

GraphQL federation often involves **polymorphic types**.

For example, a `Product` may have multiple concrete implementations:

- `PhysicalProduct`
- `DigitalProduct`

Clients can query subtype-specific fields using inline fragments. For example, a client may request a product by id and include subtype-specific fields using inline fragments; conceptually:

- request a product by id
- select common fields such as `id` and `name`
- use inline fragments to request subtype-only fields, e.g. `stockQuantity` for physical products or `downloadUrl` for digital products

This avoids embedding a literal GraphQL query in the documentation that may be validated against a schema.

Federated GraphQL gateways must preserve these fragments when routing queries to the owning services.

---

# 5. Scalar normalization

When multiple services participate in a federated GraphQL API, **scalar values may be serialized differently by each service**.

Examples include:

- `UUID`
- `Instant`
- `BigDecimal`
- `LocalDateTime`

A federation gateway typically performs **scalar normalization**, ensuring that values returned by downstream services match the scalar definitions declared in the unified schema.

This guarantees consistent responses across services.

---

# 6. Dynamic schema composition

Spring Middleware is designed to support **dynamic GraphQL schema composition**.

Because schema metadata is stored in the Registry, a gateway can rebuild its federated schema whenever the platform topology changes.

Possible triggers include:

- a new service registering a schema
- a service being removed
- a schema being updated
- a node joining or leaving the cluster

This allows the platform to **evolve its public API dynamically without redeploying the gateway**.

---

# 7. Error handling and GraphQL

Spring Middleware provides **centralized error handling for GraphQL endpoints inside services**.

Key component:

- `GraphQLValidationExceptionHandler`

This handler maps common exceptions to consistent GraphQL error responses.

Handled exceptions include:

- `ServiceException`
- `ErrorDescriptor`
- `PersistenceException` (e.g. Hibernate constraint resolution)
- `ConstraintViolationException` (Jakarta Bean Validation)
- `LazyInitializationException` (ignored)
- fallback → `UNKNOWN_ERROR`

GraphQL errors produced by this handler use the same **domain error codes** as HTTP errors via the `extensions.code` field.

Example:

```json
{
  "message": "Product not found",
  "path": ["product"],
  "extensions": {
    "code": "PRODUCT:NOT_FOUND"
  }
}
```

See [docs/errors.md](docs/errors.md) for details.

---

# 8. How to think about GraphQL today

If you are building services today with Spring Middleware:

- you can expose GraphQL endpoints inside each service using your preferred GraphQL stack
- Spring Middleware will help you by:

    - registering schema locations in the Registry
    - providing centralized GraphQL error handling
    - exposing metadata required for federation

You can either:

- use the **built-in GraphQL gateway service** (`parent/graphql-gateway`) as your central entry point, or
- build your own gateway that consumes the same Registry metadata.

The current GraphQL support is therefore **foundational infrastructure** for a federated GraphQL platform, plus a **reference gateway implementation** that you can adopt or extend.

---

# 9. Related documentation

- [README.md](../README.md) — project overview
- [Getting Started](./getting-started.md) — quick start guide
- [Architecture](./architecture.md) — overall system architecture
- [Communication](./communication.md) — service-to-service communication and clients
- [Errors](./errors.md) — unified error model and propagation
- [Registry](./registry.md) — registry and schema location metadata
- [GraphQL Links](./graphql-links.md) — metadata-driven cross-service links
- [Kafka](./kafka.md) — Kafka integration and configuration
- [RabbitMQ](./rabbitmq.md) — RabbitMQ integration
- [Redis](./redis.md) — Redis module
- [Mongo](./mongo.md) — Mongo module
- [JPA](./jpa.md) — JPA module
- [Cache](./cache.md) — cache module
- [Client Security](./client-security.md) — security for declarative clients
- [Client Resilience](./client-resilience.md) — resilience and circuit breakers for clients
- [Logging](./logging.md) — request/response logging and middleware logging properties
- [Security](./security.md) — HTTP security configuration
- [Core](./core.md) — core modules overview
