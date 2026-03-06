# Registry

This document describes the **Registry Service** used by Spring Middleware.

The Registry is the **control plane** of the platform: it maintains global knowledge about services, nodes, REST resources, and GraphQL schemas.

---

## 1. Purpose

The Registry provides a **central source of truth** for platform topology and API metadata.

It allows Spring Middleware to:

- discover services and their endpoints
- understand which nodes are running for each service (cluster)
- keep track of registered REST resources
- keep track of registered GraphQL schema locations
- detect and remove dead or unhealthy nodes

Application services themselves remain independent and stateless regarding topology – they delegate discovery and registration to the Registry.

---

## 2. Core concepts

### 2.1 Cluster

A **cluster** is a logical service identifier.

Typical examples:

- `product-service`
- `catalog-service`
- `order-service`

A cluster usually corresponds to:

- a Kubernetes `Service`, or
- a logical microservice name in any deployment environment.

### 2.2 Node

A **node** is a running instance of a cluster.

Examples:

- a Kubernetes Pod
- a Docker container
- a VM instance
- a bare IP + port endpoint

Clusters often have multiple nodes for scalability and redundancy.

### 2.3 Resource

A **resource** is a REST endpoint implemented in a service and registered in the Registry.

Spring Middleware typically marks resources with annotations such as:

- `@Register`
- `@RestController`

This allows the Registry to maintain a catalog of APIs available on each service.

### 2.4 GraphQL Schema

A **GraphQL schema** is the type and operation definition for a GraphQL API exposed by a service.

For GraphQL, the Registry does not execute queries itself; it stores **metadata** describing:

- where each schema is located
- which service and nodes serve it

This is represented by the `SchemaLocation` model.

---

## 3. Data model

The Registry uses a small set of models to represent topology and metadata. Two key models are `RegistryEntry` and `SchemaLocation`.

### 3.1 RegistryEntry

`RegistryEntry` represents a service registered in the Registry.

It typically contains fields such as:

- `clusterEndpoint` – the logical endpoint or base URL for the service cluster.
- `nodeEndpoints` – a collection of endpoints for individual nodes.
- `publicEndpoint` – an optional public-facing endpoint (e.g. behind a gateway).

From the perspective of service communication:

- `clusterEndpoint` and `nodeEndpoints` are used by `@MiddlewareClient` to resolve where to send calls.
- `publicEndpoint` can be used by gateways or external consumers.

### 3.2 SchemaLocation

`SchemaLocation` represents GraphQL schema metadata.

Key fields include:

- `namespace` – logical namespace or domain of the schema.
- `location` – location of the schema definition (e.g. classpath resource, file, remote URL).
- `contextPath` – HTTP context path for the service.
- `pathApi` – specific HTTP path for the GraphQL endpoint.
- `schemaLocationNodes` – nodes that expose this schema.

This information is used by:

- platform tools and gateways to **discover** GraphQL schemas.
- future federation components (e.g. a GraphQL gateway using Braid) to compose a unified schema.

---

## 4. Responsibilities

The Registry has several core responsibilities:

1. **Maintain service topology**  
   - keep a registry of clusters and nodes
   - track node endpoints and health

2. **Service discovery**  
   - provide lookup for clusters and nodes to clients such as `@MiddlewareClient`

3. **Node health checks and cleanup**  
   - detect dead nodes
   - remove or mark unhealthy instances so they are not used for new calls

4. **REST API metadata**  
   - store metadata about registered REST resources
   - enable inspection and tooling around REST APIs (e.g. documentation, gateways)

5. **GraphQL schema locations**  
   - store `SchemaLocation` entries
   - provide the foundation for GraphQL discovery and future federation

---

## 5. Service bootstrapping and registration

When a microservice using Spring Middleware starts up, it interacts with the Registry as part of its bootstrapping process.

### 5.1 Bootstrapping flow

A typical startup sequence looks like this:

1. **Spring Boot initialization**  
   The application context is created and beans are initialized.

2. **Annotation scanning**  
   Spring Middleware scans the application for relevant annotations, such as:
   - `@Register` on REST controllers
   - potentially other registration-related annotations

3. **REST resource registration**  
   Discovered resources are registered in the Registry, including:
   - HTTP paths
   - HTTP methods
   - service/cluster association

4. **GraphQL schema registration**  
   If the service exposes GraphQL schemas, `SchemaLocation` entries are created and stored in the Registry.

5. **Node registration**  
   The running instance registers itself as a **node** of its cluster, including:
   - node endpoint (host, port, context path)
   - health check information or capabilities

6. **Topology consistency scheduling**  
   A scheduler periodically validates Registry consistency and may:
   - re-register resources or nodes if they are missing
   - remove dead nodes

This process is largely automatic and annotation-driven.

---

## 6. Service discovery and communication

The Registry is used by the communication layer of Spring Middleware to resolve where to send service-to-service calls.

### 6.1 Using the Registry with @MiddlewareClient

When your code calls a method on a `@MiddlewareClient` interface:

1. The `service` attribute on `@MiddlewareClient` identifies the **cluster**.
2. The communication layer queries the Registry for the corresponding `RegistryEntry`.
3. From `RegistryEntry.nodeEndpoints`, a node selection strategy picks one (or more) node endpoints.
4. The actual HTTP request is sent to the selected node.

If the Registry has removed a node due to failed health checks or missing heartbeat, that node will no longer be selected.

### 6.2 Impact on error handling

If a cluster or node cannot be found in the Registry, the communication layer can:

- surface a **structured error** indicating service unavailability
- include details in the error `extensions` such as:
  - missing service/cluster name
  - attempted lookup

See `docs/errors.md` and `docs/communication.md` for more details on error propagation and client behavior.

---

## 7. GraphQL integration

For GraphQL, the Registry focuses on **schema discovery**, not on execution.

### 7.1 Schema registration

Each service that exposes GraphQL schemas registers one or more `SchemaLocation` entries.

This allows platform tools or a separate GraphQL gateway to:

- find all schemas available in the system
- map schemas to services and nodes
- load and compose schemas if needed

### 7.2 Future GraphQL gateway

A future GraphQL gateway service can:

1. Query the Registry for all `SchemaLocation` entries.
2. Use a library like **Braid** to:
   - load each service’s schema
   - build a unified schema
   - route incoming GraphQL queries to the right services.

Spring Middleware’s role is to **expose accurate and up-to-date schema metadata** through the Registry.

For more details on GraphQL, see `docs/graphql.md`.

---

## 8. Registry implementation modules

In the Spring Middleware repository, the Registry is typically implemented by a set of modules under the `registry` group, for example:

- `registry-model` – shared data model for registry entries and schema locations.
- `registry-service` – core registry logic and persistence.
- `registry-boot` – boot application to run the Registry service as a standalone microservice.

These modules are published and managed via the `io.github.spring-middleware:bom` Bill of Materials.

Consumers of Spring Middleware generally:

- run the Registry service as part of their platform infrastructure
- point their microservices at the Registry via configuration

See the `parent/registry` module in the repository for implementation details.

---

## 9. Configuration and operations (overview)

The exact configuration for the Registry depends on your environment, but typically includes:

- **Registry service URL** – where microservices can reach the Registry.
- **Authentication / authorization** (if enabled) – how services are allowed to register and query.
- **Health check configuration** – how often nodes are checked and when they are considered dead.
- **Persistence** – how registry data is stored (database, in-memory, etc.).

From the perspective of a microservice using Spring Middleware, the most important configuration is **how to connect to the Registry**. This is usually provided via Spring Boot properties or environment variables.

Operationally, the Registry should be treated as a **critical infrastructure component**:

- deploy it redundantly when needed
- monitor its health and latency
- back up its persistent state if applicable

---

## 10. Related documentation

- `README.md` – high-level overview and architecture diagram.
- `AI_CONTEXT.md` – concise description of registry responsibilities.
- `docs/architecture.md` – overall architecture and registry’s role in the control plane.
- `docs/communication.md` – how service clients use the Registry for discovery.
- `docs/graphql.md` – how GraphQL schemas use `SchemaLocation` and discovery.
- `parent/registry/*` – implementation details for the registry service and model.
