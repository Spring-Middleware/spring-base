# Spring Middleware Overview (RAG-Friendly)

## Quick Answer

**What is Spring Middleware?**
Spring Middleware is a foundation layer that sits between your Application Business Logic and Spring Boot. It standardizes microservice infrastructure (discovery, HTTP clients, GraphQL federation, tracing, etc.) so teams don't have to reimplement infrastructure patterns in every service.

**How do I install Spring Middleware?**
You add the BOM to your Maven dependency management.

**Maven `pom.xml` Example:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.spring-middleware</groupId>
            <artifactId>bom</artifactId>
            <version>1.6.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Constraints:**
- Spring Middleware is NOT an alternative to Spring Boot; it is layered on top of Spring Boot (requires Spring Boot 3.4.2 and Java 21+).
- Current version is `1.6.0`.

---

## Capabilities

### What features does Spring Middleware provide out of the box?
- Service discovery and central API metadata registry.
- Declarative HTTP clients to communicate between services.
- GraphQL schema federation and a reference GraphQL Gateway Service.
- Distributed error propagation and unified error model.
- Request context propagation (tracing across services).
- Dynamic Search engines for MongoDB and JPA.
- Redis abstractions (caching, distributed locks).
- Kafka and RabbitMQ event integration.

**Constraints:**
- It uses a modular structure; each feature is typically a distinct module that you opt into (e.g. `parent/mongo`, `parent/kafka`, `parent/graphql`).

---

## Architectural Layering

### Where does Spring Middleware fit in my architecture?
It provides the shared infrastructure.

**Architecture diagram:**
```text
Application Business Logic 
        ↓ (Uses APIs)
Spring Middleware         
        ↓ (Provides extensions)
Spring Boot / Infrastructure 
```

**Constraints:**
- Your services remain autonomous. Modules integrate natively as Spring `@AutoConfiguration` or typical starters, ensuring the architecture remains explicit.
