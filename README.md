# Spring Middleware

Spring Middleware is not another framework on top of Spring Boot.

It is a platform layer designed to standardize microservice infrastructure while keeping services autonomous and architecture explicit.


[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-green.svg)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.spring-middleware/bom.svg)](https://central.sonatype.com/artifact/io.github.spring-middleware/bom)
[![Status](https://img.shields.io/badge/status-active%20development-brightgreen)](#)
[![Architecture](https://img.shields.io/badge/Architecture-Microservices%20Platform-blueviolet.svg)](#architecture)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Current Version:** `1.1.0`  
**Status:** Active Development

**Spring Middleware** is a modular framework designed to build **consistent, resilient, and discoverable Spring Boot microservices**.

It provides infrastructure for:

- service discovery
- declarative HTTP clients
- API metadata registry
- GraphQL schema federation
- distributed error propagation
- request context propagation
- dynamic search engines (Mongo / JPA)
- Redis abstractions and distributed locks
- RabbitMQ event integration

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
- [Modules](#modules)
  - [Redis Module](#redis-module)
  - [Mongo Search Engine](#mongo-search-engine)
  - [JPA Search Engine](#jpa-search-engine)
  - [RabbitMQ Integration](#rabbitmq-integration)
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
- **schema federation**
- **distributed data utilities**

For a deeper architectural view, see `docs/architecture.md`.

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

### A Platform That Evolves

Spring Middleware is designed as an **evolving microservice platform**.  
Current capabilities already provide a solid infrastructure foundation, but additional platform features are planned.

Future capabilities include:

- **declarative security for service clients**
- **advanced resilience and retry strategies**
- **improved GraphQL federation**
- **observability and tracing integration**
- **Kubernetes-native service discovery**
- **API governance and metadata evolution**

The goal is to progressively build a **cohesive platform for Spring Boot microservices**, reducing infrastructure complexity while preserving flexibility and architectural clarity.


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
                      ┌──────────────────────┐
                      │   Registry Service   │
                      │                      │
                      │  - Service topology  │
                      │  - API metadata      │
                      │  - GraphQL schemas   │
                      │  - Node health       │
                      └──────────┬───────────┘
                                 │
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
            ▼                    ▼                    ▼

      ┌────────────┐      ┌────────────┐      ┌────────────┐
      │  Service A │      │  Service B │      │  Service C │
      │            │      │            │      │            │
      │ @Register  │      │ @Register  │      │ @Register  │
      │ GraphQL    │      │ GraphQL    │      │ GraphQL    │
      │ Middleware │      │ Middleware │      │ Middleware │
      │ Client     │◄────►│ Client     │◄────►│ Client     │
      └────────────┘      └────────────┘      └────────────┘
```

Each service automatically:

- registers its REST resources
- registers its GraphQL schemas
- reports running nodes
- participates in topology updates
- communicates via middleware clients

See `docs/architecture.md` for a full architecture description.

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

See `docs/communication.md` for more details on declarative clients and context propagation.

---

# Request Context Propagation

Each request carries two identifiers:

| Header | Description |
|------|-------------|
| `X-Request-ID` | Global request identifier across services |
| `X-Span-ID` | Local span identifier inside the service |

### Example propagation

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

   │
   ▼
Service C
requestId = 4C7F...
spanId = C11A...
```

This enables **traceability across the entire request chain** without requiring full tracing infrastructure.

---

# Error Propagation

Spring Middleware provides a **unified error model** across all services.

Errors propagate through service calls while preserving context.

### Example propagation chain

```
Client
  │
  ▼
Service A
  │
  ▼
Service B
  │
  ▼
Service C
  │
  ▼
Service D (error occurs)
```

Returned error contains the entire call chain.

### Example response

```json
{
  "statusCode": 404,
  "statusMessage": "Not Found",
  "code": "PRODUCT:NOT_FOUND",
  "message": "Product not found",
  "extensions": {
    "requestId": "F4D29AAFE7FC4844A1FF8794F186B102",
    "span": [
      {
        "service": "catalog",
        "method": "replaceProducts",
        "httpStatus": 404
      }
    ]
  }
}
```

See `docs/errors.md` for a full description of the error model and GraphQL error handling.

---

# GraphQL Support

Spring Middleware includes infrastructure for **GraphQL microservice composition**.

Each service can expose GraphQL schemas which are automatically registered.

The registry keeps track of:

- schema namespaces
- schema locations
- nodes exposing schemas

Future capabilities include:

- GraphQL schema federation
- automatic cross-service query linking
- schema stitching

Example concept:

```java
@GraphQLLink(
    schema = "company",
    type = "Vendor",
    query = "vendor"
)
```

See `docs/graphql.md` for more information about GraphQL support.

---

# Modules

Spring Middleware is distributed as multiple Maven artifacts managed by the `io.github.spring-middleware:bom` Bill of Materials.

See `docs/registry.md` and `docs/graphql.md` for platform-level registry and GraphQL behavior.

## Core

- `commons` – shared utilities used across modules
- `api` – shared API contracts
- `app` – middleware runtime (registry integration, clients, error handling)
- `model-api`, `model-core` – domain model helpers and auditing
- `view-api`, `view-core` – view-layer abstractions

## Data

- `mongo-api`, `mongo-core`, `mongo-core-commons`, `mongo-core-react` – MongoDB integration and dynamic queries
- `jpa-api`, `jpa-core` – JPA/Hibernate dynamic search
- `redis-api`, `redis-core`, `redis-core-react` – Redis utilities and distributed locks
- `cache` – Spring Cache integration backed by Redis

## Messaging

- `rabbitmq` – RabbitMQ-based messaging framework

## Platform

- `registry-model`, `registry-service`, `registry-boot` – registry data model and boot application
- `graphql` – GraphQL integration and centralized error handling

Modules can be used individually or combined. See the module READMEs under `parent/*/README.md` for more details.

---

# Redis Module

Provides a high-level abstraction over Redis.

Features include:

- key-value operations
- hash structures
- distributed locking
- bulk operations
- container/unit models

Example:

```java
redisService.set(commands, "user:123", user, 3600);

User user = redisService.get(commands, "user:123", User.class);
```

---

# Mongo Search Engine

Annotation-driven dynamic Mongo queries.

Example:

```java
@MongoSearchProperty(
    value = "email",
    operationType = OperationType.LIKE
)
private String email;
```

Repositories automatically support dynamic searches.

---

# JPA Search Engine

Dynamic HQL query generation using annotations.

Example:

```java
@SearchProperty(
    value = "customer.email",
    isLike = true
)
private String customerEmail;
```

---

# RabbitMQ Integration

The framework provides a lightweight JMS-like abstraction over RabbitMQ.

Example producer:

```java
@Component
@JmsProducer
@JmsDestination(name = "event-queue")
public class EventProducer extends JmsProducerResource<EventRequest> {
}
```

For a complete description of the RabbitMQ framework, see `parent/rabbitmq/README.md`.

---

# Installation

Use the BOM to manage compatible versions of all Spring Middleware modules:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>bom</artifactId>
      <version>1.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then add the modules you need, for example:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>app</artifactId>
  </dependency>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>mongo-core</artifactId>
  </dependency>
</dependencies>
```

---

# Requirements

- Java **21 (Preview features enabled)**
- Spring Boot **3.4.2**
- Maven **3.8+**

---

# Design Patterns

| Pattern | Usage |
|------|------|
| Specification | Dynamic query engines |
| Builder | Query construction |
| Strategy | Search strategies |
| Factory | Object creation |
| Proxy | Declarative HTTP clients |
| Repository | Data access |
| Mutex | Distributed locking |

---

# Roadmap

Planned improvements:

- GraphQL federation
- Kubernetes native discovery
- distributed tracing integration
- observability tooling
- service mesh support
- API gateway module

---

# Where to go next

- Architecture: `docs/architecture.md`
- Service communication: `docs/communication.md`
- Error model: `docs/errors.md`
- GraphQL support: `docs/graphql.md`
- Registry: `docs/registry.md`

---

# License

MIT License