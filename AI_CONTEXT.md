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

The project is distributed via Maven Central BOM:

`io.github.spring-middleware:bom`

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

    @Register
    @RestController

---

# Core Concepts

## cluster

Logical service name  
(usually a Kubernetes Service)

## node

Running service instance  
(pod / container / IP)

---

## RegistryEntry

Represents a service in the registry.

Fields:

- clusterEndpoint
- nodeEndpoints
- publicEndpoint

---

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
- health check nodes
- remove dead nodes
- store REST API metadata
- store GraphQL schema locations

---

# Service Communication

Services communicate using declarative middleware clients.

Example:

    @MiddlewareClient(service = "product")

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

    Client
      ↓
    Service A (span A1)
      ↓
    Service B (span B3)
      ↓
    Service C (span C7)

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

Error payload example:

    {
      "statusCode": 404,
      "statusMessage": "Not Found",
      "code": "PRODUCT:NOT_FOUND",
      "message": "Product not found",
      "extensions": {}
    }

Remote errors preserve additional metadata:

- remote.url
- remote.method
- remote.service
- requestId
- spanId
- callChain

---

# Modules

Spring Middleware is composed of multiple modules.

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

# GraphQL Support

GraphQL infrastructure provides:

- schema registration
- namespace management
- centralized GraphQL error handling
- schema federation foundation

Example error:

    {
      "message": "Product not found",
      "path": ["product"],
      "extensions": {
        "code": "PRODUCT:NOT_FOUND"
      }
    }

---

# Recent Improvements (2026-03)

GraphQL centralized exception handling.

Main component:

    GraphQLValidationExceptionHandler

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