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

# Tech Stack

- Java 21 (preview features enabled)
- Spring Boot 3.4.x
- WebClient
- Redis (Redisson)
- MongoDB
- GraphQL
- RabbitMQ
- Maven
- Docker
- Kubernetes

---

# Architecture

Spring Middleware uses a registry-driven microservice architecture.

A central **Registry Service** maintains the global topology of the platform.

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

# Core Concepts

## cluster

Logical service identifier.

Typically corresponds to a Kubernetes Service or logical microservice name.

Examples:

- product-service
- catalog-service

## node

Running instance of a cluster.

Examples:

- Kubernetes pod
- Docker container
- VM instance
- container IP endpoint

---

# Key Annotations

Spring Middleware uses annotations to drive platform behavior.

## @Register

Marks a REST resource that should be registered in the Registry.

Example:

```java
@Register
@RestController
```

## @MiddlewareClient

Declares a declarative service client.

Example:

```java
@MiddlewareClient(service = "product")
```

Capabilities:

- registry based service discovery
- automatic endpoint resolution
- request context propagation
- structured remote error handling

---

# Registry Model

## RegistryEntry

Represents a service registered in the platform registry.

Fields:

- clusterEndpoint
- nodeEndpoints
- publicEndpoint

## SchemaLocation

Represents GraphQL schema location metadata.

Fields:

- namespace
- location
- contextPath
- pathApi
- schemaLocationNodes

---

# Registry Responsibilities

The Registry maintains the global topology of the platform.

Responsibilities:

- maintain service topology
- perform node health checks
- remove dead nodes
- store REST API metadata
- store GraphQL schema locations

---

# Service Bootstrapping

When a service starts:

1. Spring Boot initializes the application.
2. Middleware scans for annotated resources.
3. REST resources are registered in the Registry.
4. GraphQL schemas are registered.
5. The node instance registers itself under its cluster.
6. A scheduler periodically validates registry consistency.

This process is automatic and annotation-driven.

---

# Service Communication

Services communicate using declarative middleware clients.

Example:

```java
@MiddlewareClient(service = "product")
```

Capabilities:

- service discovery via registry
- automatic endpoint resolution
- request context propagation
- structured remote error handling
- retry / resilience strategies
- optional spread calls (call all nodes)

---

# Request Context Propagation

Every request propagates two identifiers:

- X-Request-ID
- X-Span-ID

Purpose:

- correlate requests across services
- lightweight distributed tracing
- debugging and log correlation

Example chain:

```
Client
  ↓
Service A (span A1)
  ↓
Service B (span B3)
  ↓
Service C (span C7)
```

---

# Error Model

Errors are propagated using a structured error model.

Core classes:

- ServiceException
- ErrorDescriptor
- ErrorMessage
- ErrorMessageFactory
- RemoteServerException
- @RestControllerAdvice

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

Remote errors preserve additional metadata:

- remote.url
- remote.method
- remote.service
- requestId
- spanId
- callChain

---

# Modules

Spring Middleware is composed of multiple modules organized as a multi-module Maven repository.

## Core

- commons
- api
- app
- model
- view

## Data

- mongo
- jpa
- redis
- cache

## Messaging

- rabbitmq

## Platform

- registry
- graphql

---

# Repository Layout

Infrastructure modules typically follow this structure:

```
module
 ├─ api
 ├─ core
 └─ core-react (optional)
```

Typical dependency hierarchy:

```
boot
 ↓
core
 ↓
api
```

Infrastructure integration:

```
service
 ↓
middleware app
 ↓
middleware infrastructure modules
```

---

# GraphQL Support

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

# Recent Improvements (2026-03)

GraphQL centralized exception handling.

Main component:

```
GraphQLValidationExceptionHandler
```

Handled exceptions:

- ServiceException
- ErrorDescriptor
- PersistenceException (Hibernate constraint resolution)
- ConstraintViolationException (Jakarta Validation)
- LazyInitializationException (ignored)
- fallback → FrameworkErrorCodes.UNKNOWN_ERROR

---

# Current Status

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
- BOM distribution via Maven Central