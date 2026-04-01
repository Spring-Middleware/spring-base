# Spring Middleware

Spring Middleware is not another framework on top of Spring Boot.

**Adoption:** Used or evaluated by 60+ companies via Maven Central

It is a platform layer designed to standardize microservice infrastructure while keeping services autonomous and architecture explicit.


[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-green.svg)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.spring-middleware/bom.svg)](https://central.sonatype.com/artifact/io.github.spring-middleware/bom)
![Status](https://img.shields.io/badge/status-active%20development-brightgreen)
[![Architecture](https://img.shields.io/badge/Architecture-Microservices%20Platform-blueviolet.svg)](#architecture)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Current Version:** `1.4.0`  
**Status:** Active Development

**Spring Middleware** is a modular framework designed to build **consistent, resilient, and discoverable Spring Boot microservices**.

It provides infrastructure for:

- service discovery
- declarative HTTP clients
- API metadata registry
- GraphQL schema federation **and a reference GraphQL gateway service**
- distributed error propagation
- request context propagation
- dynamic search engines (Mongo / JPA)
- Redis abstractions and distributed locks
- RabbitMQ event integration
- Kafka event integration

The framework emerged from real production microservices and aims to remove recurring infrastructure complexity while keeping architecture explicit and transparent.

---

## Table of Contents

- [Overview](#overview)
- [Why Spring Middleware?](#why-spring-middleware)
- [Design Principles](#design-principles)
- [Core Concepts](#core-concepts)
- [Architecture](#architecture)
- [Service Communication](#service-communication)
- [Request Context Propagation](#request-context-propagation)
- [Error Propagation](#error-propagation)
- [GraphQL Support](#graphql-support)
- [Security configuration](#security-configuration)
- [Modules](#modules)
- [Installation](#installation)
- [Requirements](#requirements)
- [Design Patterns](#design-patterns)
- [Roadmap](#roadmap)
- [Where to go next](#where-to-go-next)
- [License](#license)

---

# Overview

Spring Middleware provides the **foundation layer for microservice platforms**.

It sits between:

```
Application Business Logic
        ↓
Spring Middleware
        ↓
Spring Boot / Infrastructure
```

Instead of every microservice reimplementing infrastructure concerns, Spring Middleware provides:

- **standardized communication**
- **central service registry**
- **consistent error model**
- **context propagation**
- **schema federation and a GraphQL gateway**
- **distributed data utilities**

For a deeper architectural view, see [docs/architecture.md](docs/architecture.md).

---

## Why Spring Middleware?

Modern microservice systems repeatedly require the same infrastructure capabilities:

- service discovery
- service-to-service communication
- distributed error handling
- request tracing and context propagation
- data access utilities
- messaging integration

While Spring Boot provides excellent building blocks, teams often end up **reimplementing similar infrastructure patterns in every service**.

Spring Middleware introduces a **platform layer on top of Spring Boot** that standardizes these capabilities while keeping microservices autonomous and architecture explicit.

Instead of hiding infrastructure behind heavy frameworks, Spring Middleware focuses on:

- **explicit architecture**
- **consistent service communication**
- **transparent error propagation**
- **discoverable service topology**

This allows teams to build microservices that are easier to operate, debug, and evolve.

---

## Design Principles

Spring Middleware is built around a small set of architectural principles:

**Explicit Architecture**

Infrastructure should be visible and understandable.  
Service communication, topology, and metadata are explicitly defined and discoverable.

**Microservice Autonomy**

Each service remains independent while participating in a shared platform infrastructure.

**Infrastructure Consistency**

Common infrastructure concerns such as service discovery, error propagation, and context propagation follow consistent patterns across all services.

**Minimal Magic**

The framework avoids hidden behavior and favors explicit configuration and annotations.

**Platform Evolution**

Spring Middleware is designed as an evolving platform where new infrastructure capabilities can be added without breaking service autonomy.

---

# Core Concepts

| Concept | Description |
|------|-------------|
| Cluster | Logical service name (usually a Kubernetes service) |
| Node | Running instance of the service |
| Registry | Central topology and metadata storage |
| Resource | Registered REST endpoint |
| Schema | Registered GraphQL schema |
| MiddlewareClient | Declarative HTTP client |
| ErrorMessage | Unified error model |

---

# Architecture

```
                      ┌────────────────────────────┐
                      │   Registry Service         │
                      │                            │
                      │  - Service topology        │
                      │  - API metadata            │
                      │  - GraphQL schemas         │
                      │  - Node health             │
                      └─────────────┬──────────────┘
                                     │
                                     │
            ┌────────────────────────┼────────────────────────┐
            │                        │                        │
            ▼                        ▼                        ▼

      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
      │  Service A   │      │  Service B   │      │  Service C   │
      │              │      │              │      │              │
      │ @Register    │      │ @Register    │      │ @Register    │
      │ GraphQL      │      │ GraphQL      │      │ GraphQL      │
      │ Middleware   │      │ Middleware   │      │ Middleware   │
      │ Client       │◄────►│ Client       │◄────►│ Client       │
      └──────────────┘      └──────────────┘      └──────────────┘
```

Each service automatically:

- registers its REST resources
- registers its GraphQL schemas
- reports running nodes
- participates in topology updates
- communicates via middleware clients

See [docs/architecture.md](docs/architecture.md) for a full architecture description.

---

# Service Communication

Spring Middleware provides **declarative service-to-service communication**.

Instead of manually writing WebClient logic, services declare clients.

### Example

```java
@MiddlewareClient(service = "product")
public interface ProductsApi {

    @PostMapping("/api/v1/products/bulk")
    List<ProductDto> createProducts(ProductBulkCreateRequestDto request);

}
```

The framework automatically handles:

- service discovery
- node selection
- load distribution
- error propagation
- context propagation
- retry strategies

Security for declarative clients
--------------------------------
Declarative clients support several security modes (passthrough, api-key, OAuth2 client-credentials, none). See [docs/client-security.md](docs/client-security.md) for detailed explanation, configuration examples and the annotations you can use on proxy interfaces and methods.

---

# Request Context Propagation

Each request carries two identifiers:

| Header | Description |
|------|-------------|
| `X-Request-ID` | Global request identifier across services |
| `X-Span-ID` | Local span identifier inside the service |

Example propagation:

```
Client Request
   │
   ▼
Service A
requestId = 4C7F...
spanId = A12F...

   │
   ▼
Service B
requestId = 4C7F...
spanId = B992...
```

Note: request/response logging is configurable via `middleware.log` properties, see [docs/logging.md](docs/logging.md) for details.

---

# Error Propagation

Spring Middleware standardizes **how errors are represented and propagated** across services.

Example:

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {}
}
```

---

# GraphQL Support

Spring Middleware provides infrastructure for **distributed GraphQL federation** across microservices.

Instead of exposing a single monolithic GraphQL server, each service can publish its own schema.  
The platform then composes these schemas into a **unified GraphQL API**.

Each service registers:

- GraphQL schema namespace
- schema definition
- context path
- GraphQL endpoint path

This metadata is stored in the **Registry Service**.

The platform can then dynamically compose a **federated GraphQL gateway** that exposes a unified API to clients.

```
Client
   │
   ▼
GraphQL Gateway
   │
   ├── product-service GraphQL API
   ├── catalog-service GraphQL API
   └── inventory-service GraphQL API
```

Capabilities include:

- distributed schema federation
- cross-service queries
- centralized GraphQL error handling
- scalar normalization
- inline fragment support
- dynamic schema composition

For a detailed explanation see:

[docs/graphql.md](docs/graphql.md)

---

# Security configuration

Spring Middleware includes a pluggable HTTP security layer built on Spring Security.

Supported modes:

- `NONE`
- `BASIC_AUTH`
- `JWT`
- `OIDC`
- `API_KEY`

All modes share a unified authorization model for public and protected endpoints.

---

# Modules

Spring Middleware is split into focused Maven modules.

## Redis Module
Provides distributed locks, shared maps and caches.  
See [docs/redis.md](docs/redis.md) for details.  
See [docs/cache.md](docs/cache.md) for the cache module details and runtime API.

## Mongo Search Engine
Dynamic search capabilities on top of MongoDB.  
See [docs/mongo.md](docs/mongo.md) for details.

## JPA Search Engine
Dynamic search capabilities for relational databases.  
See [docs/jpa.md](docs/jpa.md) for details.

## RabbitMQ Integration
Opinionated RabbitMQ integration for producers and consumers.  
See [docs/rabbitmq.md](docs/rabbitmq.md) for details and [parent/rabbitmq/README.md](parent/rabbitmq/README.md) for module-specific guidance and examples.  
For implementation best-practices and handler/listener/error-handler recommendations, see [docs/rabbitmq-best-practices.md](docs/rabbitmq-best-practices.md).

## Kafka Integration
Provides auto-configuration for Kafka publishers and subscribers, topic creation support and a registry for named publishers/subscribers.  
See [docs/kafka.md](docs/kafka.md) for details.

## Client Resilience
Client resilience (circuit-breaker, bulkhead, connection parameters) is documented in [docs/client-resilience.md](docs/client-resilience.md).

## Logging
Request/response logging for the middleware is documented in [docs/logging.md](docs/logging.md).

---

# Installation

Spring Middleware is distributed via Maven Central using a BOM.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>bom</artifactId>
      <version>1.4.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## Module versions (platform BOM)

The platform BOM `io.github.spring-middleware:bom:1.4.0` manages the versions of core modules used by Spring Middleware. Key module versions included in the BOM are:

| Module property | Version |
|-----------------|---------|
| core.commons.version | 1.5.0 |
| core.view.version | 1.4.0 |
| core.cache.version | 1.4.0 |
| core.api.version | 1.4.0 |
| core.app.version | 1.7.0 |
| core.mongo.version | 1.5.0 |
| core.rabbitmq.version | 1.4.0 |
| core.kafka.version | 1.4.0 |
| core.jpa.version | 1.5.0 |
| core.redis.version | 1.4.0 |
| core.model.version | 1.4.0 |
| core.registry.version | 1.5.0 |
| core.graphql.version | 1.4.0 |
| core.graphql.gateway.version | 1.3.0 |

Use the BOM to manage consistency of module versions across your projects.

---

# Requirements

- Java 21
- Spring Boot 3.4.x
- Maven

Some modules require Redis, MongoDB or RabbitMQ.

---

# Design Patterns

Spring Middleware embraces several architectural patterns:

- **Registry-driven architecture**
- **Declarative clients**
- **Annotation-driven registration**
- **Unified error model**
- **Context propagation**

### Self-Healing Infrastructure

Spring Middleware is designed so that **platform state converges automatically to the real state of the cluster**.

The system does not rely on the Registry always containing correct information.  
Instead, services continuously reconcile the desired state of the platform.

This enables automatic recovery scenarios such as:

- loss of registry entries
- node crashes or restarts
- missing resource registrations
- missing GraphQL schema nodes
- missing messaging infrastructure

When inconsistencies are detected, services automatically:

- re-register their REST resources
- re-register GraphQL schemas
- restore node endpoints in the registry
- recreate node-scoped messaging resources

Conversely, when nodes disappear:

- their `nodeEndpoints` are removed from the registry
- node-scoped queues are cleaned up
- cluster metadata is updated
- services fail fast if no nodes remain available

This guarantees that the platform **eventually converges to a consistent topology without manual intervention**.

---

# Roadmap

Planned improvements include:

- richer GraphQL federation
- advanced client resilience strategies
- extended security for middleware clients
- observability integration

---

# Where to go next

The project contains detailed documentation under the `docs/` directory. Start with the high-level guides and then drill down into module-specific docs.

- Architecture overview: [docs/architecture.md](docs/architecture.md)
- Getting Started: [docs/getting-started.md](docs/getting-started.md)
- Registry and schema metadata: [docs/registry.md](docs/registry.md)
- Service communication and `@MiddlewareClient`: [docs/communication.md](docs/communication.md)
- Error model and propagation: [docs/errors.md](docs/errors.md)
- GraphQL support and gateway: [docs/graphql.md](docs/graphql.md)
- HTTP security configuration: [docs/security.md](docs/security.md)
- Declarative client security (client-side modes): [docs/client-security.md](docs/client-security.md)
- Kafka integration and configuration: [docs/kafka.md](docs/kafka.md)
- Request/response logging and forced logging: [docs/logging.md](docs/logging.md)

## Module-specific documentation

- Redis: [docs/redis.md](docs/redis.md)
- Mongo: [docs/mongo.md](docs/mongo.md)
- JPA: [docs/jpa.md](docs/jpa.md)
- RabbitMQ: [docs/rabbitmq.md](docs/rabbitmq.md)

## How to find code and examples

- Module READMEs: [parent/*/README.md](parent/*/README.md) (for module-specific setup and examples, e.g. [parent/graphql-gateway/README.md](parent/graphql-gateway/README.md)).
- Use your IDE to jump to implementation classes referenced from the docs.

---

# License

Spring Middleware is released under the MIT License.