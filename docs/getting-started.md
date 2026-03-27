# Getting Started with Spring Middleware

This guide explains the minimal steps to add Spring Middleware to a new or existing Spring Boot project. It shows a Maven-based quick-start using the reference project POM you provided and includes typical configuration snippets (registry, clients, Kafka, logging, and security).

This document is intentionally concise — use the linked module documents for deeper configuration details:

- docs/communication.md
- docs/registry.md
- docs/graphql.md
- docs/kafka.md
- docs/errors.md
- docs/security.md

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

See `docs/communication.md` and `docs/registry.md` for details about how services are discovered and how the registry is updated at runtime.

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

Middleware supports service-level security and client-side security for `@MiddlewareClient` (API key, passthrough, OAuth2 client credentials). Configure protected paths and clients via the modules' configuration properties. See `docs/security.md` for the up-to-date format (note: `ProtectedPathRule.enabled` was replaced by `type` with values: `NONE`, `AUTHENTICATED`, `ROLES`).

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

- Registry and control plane: `docs/registry.md`
- Communication and declarative clients: `docs/communication.md`
- GraphQL federation and links: `docs/graphql.md`
- Kafka module details and error-handling: `docs/kafka.md`
- Security configuration: `docs/security.md`
- Error handling and exceptions: `docs/errors.md`

This file is a compact reference to get a service running with Spring Middleware quickly. For deeper configuration and examples consult the module-level docs listed above.
