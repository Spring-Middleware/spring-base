# GraphQL Support

This document describes the current and planned GraphQL support in Spring Middleware.

At this stage, Spring Middleware focuses on **schema registration and discovery**, not on executing or federating GraphQL queries itself.

---

## 1. Current capabilities

### 1.1 Schema location registration

When a service exposes a GraphQL schema, Spring Middleware can register **where** that schema lives so that other platform components can discover it later.

The registry keeps track of:

- the **namespace** of each schema
- the **location** of the schema file or endpoint
- the **HTTP context path** and API path
- the **nodes** that serve that schema

This information is represented by the `SchemaLocation` model.

In practice this means:

- each service that participates in GraphQL can register its schema locations in the Registry
- other tools or services can later read this information and decide how to compose or federate schemas

Spring Middleware itself does **not** currently:

- expose a central GraphQL gateway
- perform schema stitching or query planning
- execute GraphQL queries across services

It focuses on providing **reliable metadata** about where schemas are and which services own them.

### 1.2 SchemaLocation model

The `SchemaLocation` model describes GraphQL schema metadata stored in the Registry. Key fields include:

- `namespace` – logical namespace or domain of the schema
- `location` – location of the schema definition (e.g. classpath resource, file, URL)
- `contextPath` – HTTP context path where the service is exposed
- `pathApi` – specific HTTP path for the GraphQL endpoint
- `schemaLocationNodes` – nodes that expose this schema

Services typically do not interact directly with `SchemaLocation`; instead, they rely on Spring Middleware’s registry integration to publish schema metadata.

---

## 2. Future federation and composition

Spring Middleware is designed so that **another service** can consume the registered schema locations and build a unified GraphQL API.

A typical future setup could look like this:

1. Each microservice registers its GraphQL schema location in the Registry.
2. A **separate GraphQL gateway service** reads all `SchemaLocation` entries from the Registry.
3. That gateway uses a GraphQL federation/merging library (for example **Braid**) to:
   - load individual service schemas
   - compose them into a single unified schema
   - route queries to the appropriate backend services.

In this model:

- Spring Middleware is responsible for **schema metadata and discovery**.
- The gateway is responsible for **federation and execution**.

This separation keeps the core middleware focused on registry and communication concerns, while allowing different teams to choose or evolve their GraphQL gateway implementation.

---

## 3. Error handling and GraphQL

Although Spring Middleware does not execute federated GraphQL queries itself, it does provide **centralized error handling** for GraphQL endpoints within a service.

Key component:

- `GraphQLValidationExceptionHandler` – a handler that maps common exceptions to consistent GraphQL error responses.

Handled exceptions include:

- `ServiceException`
- `ErrorDescriptor`
- `PersistenceException` (e.g. Hibernate constraint resolution)
- `ConstraintViolationException` (Jakarta Bean Validation)
- `LazyInitializationException` (ignored)
- fallback → `UNKNOWN_ERROR` (for unexpected failures)

GraphQL errors produced by this handler use the same **domain error codes** as HTTP errors, via the `extensions.code` field. See `docs/errors.md` for details.

---

## 4. How to think about GraphQL today

If you are building services today with Spring Middleware:

- you can **expose GraphQL endpoints** within each service as usual (using your preferred GraphQL stack)
- Spring Middleware will help you by:
  - registering schema locations in the Registry via `SchemaLocation`
  - applying **centralized error handling** so errors have consistent codes and shapes
- you should plan for a **separate GraphQL gateway** (for example based on Braid) that will:
  - read schema locations from the Registry
  - compose them into a unified schema
  - act as the entry point for clients who need a single GraphQL endpoint

The current GraphQL support is therefore **foundational**: it provides the metadata and error handling needed for a future federated GraphQL layer, without forcing a specific gateway implementation today.

---

## 5. Related documentation

- `README.md` – high-level overview and GraphQL concept snippet.
- `AI_CONTEXT.md` – concise summary of GraphQL support and recent improvements.
- `docs/registry.md` – details on `SchemaLocation` and registry responsibilities.
- `docs/errors.md` – unified error model and GraphQL error representation.
