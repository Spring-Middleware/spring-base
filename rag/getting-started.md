# Getting Started (RAG-Friendly)

## Quick Answer

**How do I initialize Spring Middleware in a Spring Boot service?**
Import the Base BOM `io.github.spring-middleware:bom` inside your Maven `<dependencyManagement>` element and add the distinct modules to your service `<dependencies>`.

**Maven XML:**
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

<dependencies>
    <dependency>
        <groupId>io.github.spring-middleware</groupId>
        <artifactId>app</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.spring-middleware</groupId>
        <artifactId>kafka-core</artifactId>
    </dependency>
</dependencies>
```

**Constraints:**
- Your project uses standard Spring Boot structure. The `app` module registers interceptors and beans via standard AutoConfigurations automatically. No special main class is required.

---

## Service Registration

### How do I join the Microservice Mesh Control Plane?
Enable the `registry` option in your properties file and provide the public registry endpoint address. 
Also, use `@Register` inside classes that you want exposed automatically to other peers.

**YAML Configuration:**
```yaml
spring:
  middleware:
    registry:
      url: ${REGISTRY_ENDPOINT:http://localhost:8080/registry}
      enabled: true
```

**Java code:**
```java
import io.github.spring.middleware.annotation.Register;

@RestController
@Register
@RequestMapping("/catalogs")
public class CatalogController { ... }
```

---

## Initial Core Setup

### What properties do I typically configure?
You define common controls directly inside `application.yml` targeting Client Resilience, Security, Logs, and Kafka parameters.

**YAML Configuration:**
```yaml
middleware:
  log:
    apiKey: ${MIDDLEWARE_LOG_API_KEY:}
  kafka:
    enabled: true
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  client:
    registry-endpoint: http://registry-service:8080/registry
    product: # Declarative client "product"
      connection:
        timeout: 500
```

**Constraints:**
- When properties aren't defined properly or fallback strings are missing, context loading may throw initialization binding failures. For test modules, it is common to disable property validation globally.
