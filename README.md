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
- [Security configuration](#security-configuration)
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

The same identifiers are forwarded across middleware clients so that logs and errors can be correlated end-to-end.

For more details, see `docs/communication.md` and `docs/errors.md`.

---

# Error Propagation

Spring Middleware standardizes **how errors are represented and propagated** across services.

All HTTP and GraphQL errors share a common JSON structure represented by the `ErrorMessage` type.

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

When a service calls another service via a middleware client, remote errors are mapped to `RemoteServerException` and
preserve metadata such as:

- `remote.url`
- `remote.method`
- `remote.service`
- `requestId`
- `spanId`
- `callChain`

See `docs/errors.md` for a complete description of the error model and propagation rules.

---

# GraphQL Support

Spring Middleware provides infrastructure for **GraphQL schema registration and federation**.

Each service can register its GraphQL schemas in the Registry, including:

- schema namespace
- schema location
- context path
- API path

The Registry stores this metadata so that a separate GraphQL gateway can later compose a federated schema (for example
using tools like Braid).

Errors raised by GraphQL resolvers are mapped into the same error codes and messages documented in `docs/errors.md`,
using centralized exception handling.

See `docs/graphql.md` for more details.

---

# Security configuration

Spring Middleware includes a pluggable HTTP security layer built on Spring Security 6.

Security is configured per service through the `middleware.security.*` properties and supports multiple modes via the
`SecurityType` enum:

- `NONE` – security disabled, all requests are permitted.
- `BASIC_AUTH` – HTTP Basic authentication backed by configuration or a `UserApi`.
- `JWT` – stateless JWT resource server.
- `OIDC` – OIDC-based resource server backed by an external identity provider.
- `API_KEY` – header-based API key authentication with role-based access control.

All modes share a common authorization model based on **public** and **protected** paths, so you can configure which
endpoints are open and which require specific roles.

For a detailed guide and configuration examples (including API key security), see `docs/security.md`.

---

# Modules

Spring Middleware is split into focused Maven modules so you can depend only on what you need.

## Redis Module

The Redis module provides **high-level abstractions for Redis** using Redisson.

Features include:

- distributed locks
- shared maps and caches
- queue abstractions

See `parent/redis/README.md` for module-specific details.

---

## Mongo Search Engine

The Mongo module provides **dynamic search capabilities** on top of MongoDB.

It allows services to build flexible query APIs without exposing raw query languages.

See `parent/mongo/README.md` for more information.

---

## JPA Search Engine

The JPA module provides **dynamic search capabilities** using JPA / Hibernate.

It mirrors the Mongo search engine so services can offer consistent search APIs regardless of the underlying datastore.

See `parent/jpa/README.md` for more information.

---

## RabbitMQ Integration

The RabbitMQ module provides **opinionated integration** with RabbitMQ.

It focuses on:

- simplified configuration
- standard patterns for producers and consumers
- integration with the error and tracing model

See `parent/rabbitmq/README.md` for module-specific documentation.

---

# Installation

Spring Middleware is distributed via Maven Central using a BOM.

Add the BOM to your project:

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

Then add the modules you need, for example the `app` module:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>app</artifactId>
  </dependency>
</dependencies>
```

---

# Requirements

- Java 21 (with preview features enabled)
- Spring Boot 3.4.x
- Maven

Some modules require additional infrastructure (Redis, MongoDB, RabbitMQ, etc.).

---

# Design Patterns

Spring Middleware embraces several design patterns:

- **Registry-driven architecture** – a central Registry stores service topology and API metadata.
- **Declarative clients** – service communication is described via annotated interfaces.
- **Annotation-driven registration** – `@Register` marks resources that should be registered automatically.
- **Unified error model** – the same error representation is used across REST and GraphQL.
- **Context propagation** – request and span identifiers are propagated across service boundaries.

These patterns keep services explicit, observable, and easier to maintain.

---

# Roadmap

Planned improvements include:

- richer GraphQL federation and tooling
- advanced resilience and retry strategies for clients
- extended security features for middleware clients
- tighter integration with observability stacks (metrics, tracing, logging)

---

# Where to go next

- `docs/architecture.md` – high-level platform architecture.
- `docs/registry.md` – Registry model and registration flows.
- `docs/communication.md` – declarative service communication.
- `docs/errors.md` – unified error model.
- `docs/graphql.md` – GraphQL support and federation.
- `docs/security.md` – HTTP security configuration and examples.

---

# License

Spring Middleware is released under the MIT License. See `LICENSE` for details.

