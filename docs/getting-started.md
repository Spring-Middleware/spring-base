# Getting Started with Spring Middleware

This guide explains the minimal steps to add Spring Middleware to a new or existing Spring Boot project. It shows a Maven-based quick-start using the reference project POM you provided and includes typical configuration snippets (registry, clients, Kafka, logging, and security).

This document is intentionally concise — use the linked module documents for deeper configuration details:

- [Communication](./communication.md)
- [Registry](./registry.md)
- [GraphQL](./graphql.md)
- [Kafka](./kafka.md)
- [Errors](./errors.md)
- [Security](./security.md)

## 1. Two integration options

1. Use the project-level dependency management (recommended for reference projects)
2. Add only the specific modules you need (lightweight)

### 1.1 Option A — Use the reference POM (recommended)

If you maintain a multi-module project, you can reuse the reference POM's dependencyManagement to import the middleware versions. Example (excerpt from your reference POM):

```xml
<dependencyManagement>
  <dependencies>
    <!-- Spring Boot BOM -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring.boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>

    <!-- Spring Middleware modules (managed versions) -->
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>api</artifactId>
      <version>${spring-middleware.api.version}</version>
    </dependency>
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>app</artifactId>
      <version>${spring-middleware.app.version}</version>
    </dependency>
    <!-- add other modules as needed (kafka-core, graphql, mongo-core...) -->
  </dependencies>
</dependencyManagement>
```

Then add the runtime dependencies you need in each module's `dependencies` section. Example: add the `app` starter and Kafka core to a service:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>app</artifactId>
  </dependency>

  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>kafka-core</artifactId>
  </dependency>

  <!-- your service modules (api/core/boot) -->
</dependencies>
```

### 1.2 Option B — Add only the modules you need

If you prefer not to import the full reference dependency management, add explicit versions for the middleware modules you use (not shown here to keep the example small). Prefer stable managed versions listed in the repository BOM.

## 2. Minimal application structure

A typical service using Spring Middleware contains these modules (example names):

- `*-api` — shared DTOs and contracts
- `*-core` — business logic
- `*-boot` — Spring Boot application artifact that depends on `app` (middleware starter) and `*-core`

Your `*-boot` module `pom.xml` should include the middleware `app` starter (see previous section).

## 3. Example Spring Boot application class

No special main class is required beyond a normal Spring Boot `@SpringBootApplication`. The middleware modules provide auto-configuration when you add the corresponding dependencies.

```java
@SpringBootApplication
public class CatalogApplication {
    static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
```

## 4. Declarative clients and registration

Spring Middleware provides declarative clients and automatic registry registration.

Example `@MiddlewareClient` interface:

```java
@MiddlewareClient(service = "product")
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable("id") UUID id);
}
```

Registering a REST controller so it appears in the Registry:

```java
@RestController
@Register
@RequestMapping("/catalogs")
public class CatalogController {
    // ... endpoints ...
}
```

See [Communication](./communication.md) and [Registry](./registry.md) for details about how services are discovered and how the registry is updated at runtime.

## 5. Common configuration snippets (application.yml)

Below are short examples of typical properties used by middleware modules. Adapt to your environment.

- Registry / control plane (example):

```yaml
spring:
  middleware:
    registry:
      url: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
      enabled: true
```

- Declarative client (example per-service override):

```yaml
middleware:
  client:
    registry-endpoint: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
    product:
      security:
        type: OAUTH2_CLIENT_CREDENTIALS # NONE, PASSTHROUGH, API_KEY, OAUTH2_CLIENT_CREDENTIALS
        api-key: ${API_KEY_PRODUCT_SERVICE:default-product-api-key}
        oauth2:
          client-id: ${OAUTH2_CLIENT_ID_PRODUCT_SERVICE:product-service}
          client-secret: ${OAUTH2_CLIENT_SECRET_PRODUCT_SERVICE}
          token-uri: ${OAUTH2_TOKEN_URI_PRODUCT_SERVICE:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/token}
```

- Kafka (topics, publishers, subscribers):

```yaml
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  create-missing-topics: true
  topics:
    catalog-events:
      partitions: 5
      replication-factor: 3
  publishers:
    catalog:
      topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
  subscribers:
    catalog:
      group-id: ${KAFKA_GROUP_ID_CATALOG:catalog-service-group}
      topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
      concurrency: 3
```

- Kafka error handling (optional):

```yaml
kafka:
  error-handling:
    enabled: ${KAFKA_ERROR_HANDLING_ENABLED:true}
    max-retries: ${KAFKA_ERROR_HANDLING_MAX_RETRIES:3}
    retry-backoff-ms: ${KAFKA_ERROR_HANDLING_RETRY_BACKOFF_MS:1000}
    dead-letter:
      enabled: ${KAFKA_ERROR_HANDLING_DEAD_LETTER_ENABLED:true}
      suffix: .DLT
```

- Middleware logging: forcing request logs using `middleware.log.apiKey` and the `X-Logging-Key` header is supported via the `RequestLoggingFilter`. Example:

```yaml
middleware:
  log:
    apiKey: ${MIDDLEWARE_LOG_API_KEY:}
```

If a request includes header `X-Logging-Key` with the configured value, request/response payloads will be recorded even when the global log level is INFO/WARN.

## 6. Security and protected paths

Middleware supports service-level security and client-side security for `@MiddlewareClient` (API key, passthrough, OAuth2 client credentials). Configure protected paths and clients via the modules' configuration properties. See [Security](./security.md) for the up-to-date format (note: `ProtectedPathRule.enabled` was replaced by `type` with values: `NONE`, `AUTHENTICATED`, `ROLES`).

## 7. Build and run

Build a module and run the Boot jar from the `*-boot` module:

```bash
mvn -T 1C -DskipTests clean package
java -jar target/your-service-boot-<version>.jar
```

Or run with your IDE using the usual Spring Boot run configuration.

## 8. Useful tips and troubleshooting

- If Docker builds fail copying artifacts, check your `.dockerignore` — build context must include the target JAR path used by the Dockerfile.
- Use the BOM and dependencyManagement to keep middleware module versions consistent across modules.
- Enable `create-missing-topics` for local development but prefer to manage topics in production (with explicit partitions/replication configured).
- When running tests that rely on configuration properties, ensure required property beans (e.g. `KafkaProperties`) can bind — add minimal properties in the test config or disable validation if appropriate.

## 9. Next steps / Links

- [Registry and control plane](./registry.md)
- [Communication and declarative clients](./communication.md)
- [GraphQL federation and links](./graphql.md)
- [Kafka module details and error-handling](./kafka.md)
- [Security configuration](./security.md)
- [Error handling and exceptions](./errors.md)

This file is a compact reference to get a service running with Spring Middleware quickly. For deeper configuration and examples consult the module-level docs listed above.

## Minimal reference-service example

Below is a concrete minimal example for a `reference-service` aggregator and a simple `catalog-service` module. Use this as a starting point and adapt to your needs.

### Root aggregator `pom.xml` (reference-service)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.spring-middleware</groupId>
    <artifactId>reference-service</artifactId>
    <version>1.2.0</version>
    <packaging>pom</packaging>

    <name>spring-middleware-reference-service</name>
    <description>Reference microservices built on Spring Middleware</description>
    <url>https://github.com/Spring-Middleware/reference-service</url>

    <properties>

        <catalog.version>1.2.0</catalog.version>
        <product.version>1.2.0</product.version>

        <spring.boot.version>3.4.2</spring.boot.version>
        <spring-middleware.api.version>1.4.0</spring-middleware.api.version>
        <spring-middleware.app.version>1.7.0</spring-middleware.app.version>
        <spring-middleware.mongo-core.version>1.5.0</spring-middleware.mongo-core.version>
        <spring-middleware.kafka-core.version>1.4.0</spring-middleware.kafka-core.version>
        <spring-middleware.graphql.version>1.4.0</spring-middleware.graphql.version>
        <springdoc.openapi.version>2.8.4</springdoc.openapi.version>

        <java.version>21</java.version>
        <maven.compiler.release>${java.version}</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    </properties>

    <modules>
        <module>catalog-service</module>
        <module>product-service</module>
    </modules>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>io.github.spring-middleware</groupId>
                <artifactId>api</artifactId>
                <version>${spring-middleware.api.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.spring-middleware</groupId>
                <artifactId>app</artifactId>
                <version>${spring-middleware.app.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.spring-middleware</groupId>
                <artifactId>mongo-core</artifactId>
                <version>${spring-middleware.mongo-core.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.spring-middleware</groupId>
                <artifactId>kafka-core</artifactId>
                <version>${spring-middleware.kafka-core.version}</version>
            </dependency>

            <dependency>
                <groupId>io.github.spring-middleware</groupId>
                <artifactId>graphql</artifactId>
                <version>${spring-middleware.graphql.version}</version>
            </dependency>

            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.openapi.version}</version>
            </dependency>

        </dependencies>
    </dependencyManagement>

</project>
```

> Note: this `pom.xml` is based on the reference you provided (versions already set). Adjust `version` values if you want to use a different platform release.

### Minimal `catalog-service/pom.xml` snippet

Inside the module `catalog-service`, keep the `parent` reference to the aggregator and add dependencies you need. Minimal example:

```xml
<parent>
  <groupId>io.github.spring-middleware</groupId>
  <artifactId>reference-service</artifactId>
  <version>1.2.0</version>
</parent>

<dependencies>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>api</artifactId>
  </dependency>
  <dependency>
    <groupId>io.github.spring-middleware</groupId>
    <artifactId>app</artifactId>
  </dependency>
  <!-- Add other modules as needed (kafka-core, graphql, jpa, etc.) -->
</dependencies>
```

### Example Spring Boot main class (`catalog-service`)

```java
package io.github.example.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatalogApplication {
    static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
```

### Example `application.yml` for `catalog-service`

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/catalog}

spring:
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017/catalog}
      uuid-representation: standard
  kafka:
    producer:
      properties:
        max.block.ms: 10000
        delivery.timeout.ms: 10000
        request.timeout.ms: 5000

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    io.github.spring.middleware: ${LOG_LEVEL_PACKAGE:INFO}
    io.github.spring.middleware.scheduler: ERROR
    io.github.spring.middleware.jms: ERROR
    org.springframework.security: ERROR
    org.springframework.security.web: ERROR
    root: ${LOG_LEVEL_ROOT:ERROR}

middleware:
  log:
    apiKey: ${LOG_API_KEY}
    request:
      enabled: ${LOG_REQUEST_ENABLED:true}
    response:
      enabled: ${LOG_RESPONSE_ENABLED:true}
    responseTime:
      enabled: ${LOG_RESPONSE_TIME_ENABLED:false}
    exclude:
      urlPatterns:
        - /api-docs/**
        - /swagger-ui.html
        - /swagger-ui/**
        - /_alive
        - /graphql
        - /graphql/_alive
        - /graphql/schema-metadata

  jms:
    profile: ${JMS_PROFILE:LOCAL}
    host: ${JMS_HOST:amqp://rabbitmq:5672}
    user: ${JMS_USER:admin}
    password: ${JMS_PASSWORD:admin}
    max-pool-size: ${JMS_MAX_POOL_SIZE:10}
    min-idle: ${JMS_MIN_IDLE:5}
    max-idle: ${JMS_MAX_IDLE:10}
    rabbitmq:
      base-url: ${JMS_RABBITMQ_BASE_URL:http://rabbitmq:15672}
      registry:
        scanner:
          enabled: ${JMS_RABBITMQ_REGISTRY_SCANNER_ENABLED:true}
          check-interval: ${JMS_RABBITMQ_REGISTRY_SCANNER_CHECK_INTERVAL:10000}

  kafka:
    enabled: ${KAFKA_ENABLED:false}
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    create-missing-topics: true
    topics:
      catalog-events:
        partitions: 5
        replication-factor: 3

    error-handling:
      enabled: ${KAFKA_ERROR_HANDLING_ENABLED:true}
      max-retries: ${KAFKA_ERROR_HANDLING_MAX_RETRIES:3}
      retry-backoff-ms: ${KAFKA_ERROR_HANDLING_RETRY_BACKOFF_MS:1000}
      dead-letter:
        enabled: ${KAFKA_ERROR_HANDLING_DEAD_LETTER_ENABLED:true}
        suffix: .DLT

    publishers:
      catalog:
        topic: ${KAFKA_TOPIC_CATALOG:catalog-events}

    subscribers:
      catalog:
        group-id: ${KAFKA_GROUP_ID_CATALOG:catalog-service-group}
        topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
        concurrency: 3

  client:
    registry-endpoint: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
    product:
      security:
        type: OAUTH2_CLIENT_CREDENTIALS # Options: NONE, PASSTHROUGH, API_KEY, OAUTH2_CLIENT_CREDENTIALS

        api-key: ${API_KEY_PRODUCT_SERVICE:default-product-api-key}
        listProducts:
          api-key: ${API_KEY_PRODUCT_SERVICE:default-product-list-api-key}

        oauth2:
          client-id: ${OAUTH2_CLIENT_ID_PRODUCT_SERVICE:product-service}
          client-secret: ${OAUTH2_CLIENT_SECRET_PRODUCT_SERVICE}
          token-uri: ${OAUTH2_TOKEN_URI_PRODUCT_SERVICE:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/token}

      connection:
        timeout: 500
        max-retries: 0

      circuit-breaker:
        wait-duration-in-open-state-ms: 10000

  resourceRegister:
    clusterName: ${RESOURCE_CLUSTER_NAME:catalog}

  public-server:
    # In Docker the public hostname will be the service name `registry` (or set via env)
    host: ${PUBLIC_SERVER_HOST:localhost}
    port: ${PUBLIC_SERVER_PORT:8080}

  graphql:
    enabled: ${GRAPHQL_ENABLED:true}
    clusterName: ${GRAPHQL_CATALOG_CLUSTER_NAME:catalog}
    namespace: ${GRAPHQL_CATALOG_NAMESPACE:catalog}

  registry-consistency-scheduler:
    enabled: ${REGISTRY_CONSISTENCY_SCHEDULER_ENABLED:true}
    cron: ${REGISTRY_CONSISTENCY_SCHEDULER_CRON:*/10 * * * * *}

  security:
    type: ${SECURITY_TYPE:OAUTH2} # Options: NONE, BASIC_AUTH, JWT, OUAHT2, API_KEY
    public-paths:
      - /graphql
      - /resources/register
      - /api-docs/**
      - /swagger-ui.html
      - /swagger-ui/**
      - /start-publishing
      - /stop-publishing

    protected-paths:
      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ POST ]
        allowed-roles: [ ADD_PRODUCTS_TO_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ GET ]
        allowed-roles: [ LIST_CATALOG_PRODUCTS, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ DELETE ]
        allowed-roles: [ REMOVE_PRODUCTS_FROM_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*/products
        methods: [ PUT ]
        allowed-roles: [ REPLACE_PRODUCTS_FROM_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*
        methods: [ GET ]
        query-params:
          - name: expand
            required: true
            values:
              - products
        allowed-roles: [ LIST_CATALOG_PRODUCTS, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*
        methods: [ GET ]
        allowed-roles: [ GET_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*
        methods: [ PATCH, PUT ]
        allowed-roles: [ UPDATE_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs/*
        methods: [ DELETE ]
        allowed-roles: [ DELETE_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs
        methods: [ POST ]
        allowed-roles: [ CREATE_CATALOG, ADMIN ]

      - type: ROLES
        path: /api/*/catalogs
        methods: [ GET ]
        allowed-roles: [ LIST_CATALOGS, ADMIN ]

    basic-auth:
      credentials:
        - username: ${BASIC_AUTH_CATALOG_ADMIN}
          password: ${BASIC_AUTH_CATALOG_ADMIN_PASSWORD}
          roles: [ ADMIN ]

    jwt:
      secret: ${JWT_SECRET:dfwzsdzwh823zebdwdz772632gdsbddfr4}

    oauth2:
      jwk-set-uri: ${OAUTH2_ISSUER_URI_JWK_SET_URI:http://keycloak:8080/realms/spring-middleware/protocol/openid-connect/certs}
      authorities-claim-path: ${OAUTH2_ISSUER_URI_AUTHORITIES_CLAIM_PATH:$.resource_access.catalog-service.roles[*]}

    api-key:
      header-name: X-API-KEY
      credentials:
        - key: ${API_KEY_CATALOG_SERVICE:default-api-key}
          enabled: true
          roles: [ ADMIN ]

  errors:
    CATALOG_NOT_FOUND: 404

redisson:
  # In Docker compose Redis service should be named `redis`
  address: ${REDIS_ADDRESS:redis://localhost:6379}
  database: ${REDIS_DATABASE:0}
```

### Example: `catalog-service` calling `ProductApi` client

`ProductApi` is a generated declarative client (OpenAPI-generated interface annotated with middleware annotations). In `catalog-service` you typically inject and use it like any Spring bean.

```java
@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {

    private final ProductApi productApi;

    public CatalogController(ProductApi productApi) {
        this.productApi = productApi;
    }

    @GetMapping("/{id}/product/{productId}")
    public ProductDto getProductFromCatalog(@PathVariable UUID id, @PathVariable UUID productId) {
        // Example: call the product service through the generated client
        return productApi.getProduct(productId);
    }
}
```

### Build and run

- Build the whole multi-module project from the repository root:

```bash
mvn -T 1C -DskipTests clean package
```

- Run the `catalog-service` boot module (from `catalog-service` folder):

```bash
java -jar target/catalog-service-boot-<version>.jar
```

Or run the application directly from your IDE.

---

