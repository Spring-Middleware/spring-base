# Architecture

This document describes the high-level architecture of **Spring Middleware** and how its building blocks fit together.

Spring Middleware provides a **platform layer** between your business services and the underlying infrastructure:

```
Application Business Logic
        ↓
Spring Middleware (platform layer)
        ↓
Spring Boot / Infrastructure (HTTP, Data Stores, Messaging)
```

The main goals are:

- centralize cross-cutting infrastructure concerns
- keep microservice code focused on business logic
- provide consistent behavior across all services (communication, errors, tracing)

---

## 1. Architectural Overview

Spring Middleware is built around a **registry-driven microservice architecture**.

At a high level there are two planes:

- **Control plane** – the **Registry Service** that knows the platform topology (clusters, nodes, REST resources, GraphQL schemas).
- **Data plane** – application services that expose APIs, call each other, and use infrastructure modules (Mongo/JPA/Redis/RabbitMQ).

### 1.1 High-level topology

```
                      ┌──────────────────────────────┐
                      │       Registry Service       │
                      │                              │
                      │  - Service topology          │
                      │  - API metadata              │
                      │  - GraphQL schemas           │
                      │  - Node health               │
                      └──────────────┬───────────────┘
                                     │
                                     │
            ┌─────────────────────────┼─────────────────────────┐
            │                         │                         │
            ▼                         ▼                         ▼

      ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
      │  Service A   │         │  Service B   │         │  Service C   │
      │              │         │              │         │              │
      │ @Register    │         │ @Register    │         │ @Register    │
      │ GraphQL      │         │ GraphQL      │         │ GraphQL      │
      │ Middleware   │◄──────► │ Middleware   │◄──────► │ Middleware   │
      │ Clients      │         │ Clients      │         │ Clients      │
      └──────────────┘         └──────────────┘         └──────────────┘
```

Each service:

- registers itself and its APIs in the Registry
- uses **declarative clients** (`@MiddlewareClient`) to call other services
- participates in a unified **error and tracing model**

---

## 2. Core Concepts

### 2.1 Cluster and Node

- **Cluster** – logical service identifier (usually a Kubernetes Service or a logical microservice name), e.g. `product-service`, `catalog-service`.
- **Node** – running instance of a cluster (Kubernetes Pod, Docker container, VM, etc.). A cluster typically has multiple nodes for scalability and redundancy.

The Registry tracks clusters and associated nodes, along with their endpoints and health.

### 2.2 Registry

The **Registry Service** is the central control-plane component. It maintains:

- **service topology** (clusters and node endpoints)
- **REST API metadata** (registered resources)
- **GraphQL schema locations**
- **node health** and cleanup of dead nodes

The Registry exposes a model including:

- `RegistryEntry` – describes a registered service
  - `clusterEndpoint`
  - `nodeEndpoints`
  - `publicEndpoint`
- `SchemaLocation` – describes GraphQL schema metadata
  - `namespace`
  - `location`
  - `contextPath`
  - `pathApi`
  - `schemaLocationNodes`

See `docs/registry.md` for full details.

### 2.3 Resources and Schemas

- **Resource** – a REST endpoint implemented by a Spring `@RestController` and annotated with `@Register` so it is visible in the Registry.
- **Schema** – a GraphQL schema that is registered in the Registry for later composition and federation.

### 2.4 Declarative Clients

`@MiddlewareClient` defines a **declarative HTTP client** against another service. Instead of manually configuring `WebClient` or `RestTemplate`, interfaces annotated with `@MiddlewareClient` are resolved via the Registry:

- service name → cluster resolution
- node selection and load distribution
- context and error propagation

See `docs/communication.md` for more details.

---

## 3. Service Bootstrapping Flow

When a microservice using Spring Middleware starts:

1. **Spring Boot** initializes the application context.
2. Spring Middleware **scans for annotations** such as `@Register` and `@MiddlewareClient`.
3. **REST resources** are registered in the Registry (API metadata).
4. **GraphQL schemas** are registered in the Registry.
5. The running instance (**node**) registers itself under its **cluster**.
6. A **scheduler** periodically validates Registry consistency and re-registers resources if needed.

This process is transparent to the service; you activate it by including the relevant modules and annotations.

---

## 3.1 Control Plane Convergence and Self-Healing

Spring Middleware is designed so that the **control plane always converges to the real state of the cluster**.

The Registry is not treated as a permanently correct source of truth.  
Instead, services continuously **reconcile their presence and metadata** through periodic consistency checks.

This mechanism allows the platform to recover automatically from situations such as:

- lost registry entries
- registry restarts
- node crashes or restarts
- missing resource registrations
- missing GraphQL schema nodes
- missing messaging infrastructure

Each service periodically verifies that:

- its **node endpoint** exists in the Registry
- its **REST resources** are registered
- its **GraphQL schema node** is registered
- required **messaging infrastructure** exists

If inconsistencies are detected, the service automatically performs corrective actions:

- re-registering REST resources
- re-registering GraphQL schemas
- restoring node endpoints in the registry
- recreating node-scoped messaging resources (queues, bindings)

Conversely, when nodes disappear:

- their `nodeEndpoints` are removed from the registry
- node-scoped messaging queues are removed
- cluster topology is updated
- clients fail fast if no nodes are available

This design guarantees that the platform **eventually converges to a consistent topology without manual intervention**, even after partial failures of the control plane.

In practice this means:

- destroying the registry state does not permanently break the system
- restarting nodes automatically reconstructs platform metadata
- the registry topology remains clean without orphan nodes
- messaging resources are recreated automatically when nodes return
- services resume operation automatically when capacity becomes available again

---

## 4. Service Communication Flow

At runtime, services communicate through `@MiddlewareClient` interfaces.

### 4.1 From client call to remote service

1. Application code calls a method on a `@MiddlewareClient` interface.
2. The middleware resolves the **target cluster** from the annotation (e.g. `service = "product"`).
3. It queries the **Registry** for available nodes of that cluster.
4. A **node selection strategy** chooses one or more nodes (for spread calls) and resolves the HTTP endpoint.
5. A request is sent using Spring **WebClient**, including propagated headers (`X-Request-ID`, `X-Span-ID`).
6. The response is mapped back to the client method return type.

### 4.2 Context and error propagation

Every communication:

- **propagates request context** – `X-Request-ID` and `X-Span-ID` headers are passed across services
- **normalizes errors** – remote failures are converted into a unified error model and may be wrapped in a `RemoteServerException` with metadata like:
  - remote URL and HTTP method
  - remote service name
  - requestId and spanId
  - full call chain

See `docs/communication.md` and `docs/errors.md` for details.

---

## 5. Error and Tracing Architecture

Spring Middleware defines a **structured error model** implemented through:

- `ServiceException`
- `ErrorDescriptor`
- `ErrorMessage` and `ErrorMessageFactory`
- `RemoteServerException`
- global `@RestControllerAdvice`

Error responses are consistent across services, for example:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {}
}
```

In addition, every request and remote call participates in a **lightweight trace** using:

- `X-Request-ID` – end-to-end correlation ID
- `X-Span-ID` – local span ID per service

This enables debugging and log correlation without requiring a full tracing stack.

GraphQL requests participate in the same error and tracing model via centralized exception handling. See `docs/errors.md` and `docs/graphql.md` for more information.

---

## 6. Module Architecture

Spring Middleware is split into multiple modules published under the `io.github.spring-middleware` group and managed by the **BOM** (`bom` artifact).

### 6.1 Logical module groups

**Core**

- `commons`
- `api`
- `app`
- `model-api`, `model-core`
- `view-api`, `view-core`

**Data**

- `mongo-*`
- `jpa-*`
- `redis-*`
- `cache`

**Messaging**

- `rabbitmq`

**Platform**

- `registry-*`
- `graphql`

### 6.2 Infrastructure module structure

Most infrastructure modules follow this structure:

```
module
 ├─ api
 ├─ core
 └─ core-react (optional)
```

Typical dependency flow:

```
boot
 ↓
core
 ↓
api
```

Application services usually depend on `*-core` modules together with the main `app` module.

---

## 7. Deployment and Repository Layout

The Git repository is organized as a Maven multi-module project:

- Root project: **BOM** (`io.github.spring-middleware:bom`)
- `parent/` project: implementation modules (`api`, `app`, `mongo`, `jpa`, `redis`, `cache`, `registry`, `graphql`, `rabbitmq`)

For **library consumers**:

- depend on the **BOM** and specific modules from Maven Central.

For **contributors**:

- work inside the `parent/` project and build using Maven from the repository root.

---

## 8. Related Documentation

- `README.md` – project overview
- `AI_CONTEXT.md` – condensed machine-readable project summary
- `docs/getting-started.md` – quick start guide for integrating Spring Middleware (new)
- `docs/communication.md`
- `docs/errors.md`
- `docs/graphql.md`
- `docs/registry.md`
- `docs/getting-started.md`