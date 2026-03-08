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

    protected-endpoints:
      - path: /api/v1/catalogs/**
        enabled: true
        methods: [GET]
        allowed-roles: [GET_USER, ADMIN]

    basic:
      credentials:
        username: admin
        password: admin
        roles: [ADMIN]
```

Protected endpoints are evaluated dynamically during security configuration.

Authorization rules are translated into Spring Security `requestMatchers`.

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

Handled exceptions:

- `ServiceException`
- `ErrorDescriptor`
- `PersistenceException` (Hibernate constraint resolution)
- `ConstraintViolationException` (Jakarta Validation)
- `LazyInitializationException` (ignored)
- fallback → `FrameworkErrorCodes.UNKNOWN_ERROR`

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