# Spring Middleware

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

---

# Modules

The framework is organized into independent modules.

```
spring-middleware/
├── api
├── commons
├── app
├── model
├── cache
├── view
├── mongo
├── jpa
├── redis
├── rabbitmq
└── microservice-runtime
```

Modules can be used individually or combined.

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

---

# Installation

Import the BOM:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>bom</artifactId>
      <version>REPLACE_WITH_VERSION</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
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

# License

MIT License