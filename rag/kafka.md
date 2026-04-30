# Kafka Integration (RAG-Friendly)

## Quick Answer

**How to publish a Kafka event:**
Obtain `KafkaPublisherRegistry` from the application context to publish an `EventEnvelope<T>`.
```java
@Service
public class CatalogService {
    private final KafkaPublisher<CatalogEvent, String> publisher;

    public CatalogService(KafkaPublisherRegistry registry) {
        this.publisher = registry.getPublisher("catalog");
    }

    public void publishEvent(CatalogEvent event) {
        // Publishes asynchronously
        publisher.publish(event); 
        // Or publish with key: publisher.publishWithKey(event, "some-key");
    }
}
```

**How to create a Kafka subscriber:**
Use the `@MiddlewareKafkaListener` annotation matching the subscriber ID defined in your YAML.
```java
@Component
public class CatalogSubscriber {
    @MiddlewareKafkaListener("catalog")
    public void onCatalogEvent(EventEnvelope<CatalogEvent> envelope) {
        CatalogEvent payload = envelope.getPayload();
        System.out.println("Received: " + payload);
    }
}
```

**What configuration is required?**
Kafka configuration is defined under the `middleware.kafka` prefix.
```yaml
middleware:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    publishers:
      catalog:
        topic: catalog-events
    subscribers:
      catalog:
        topic: catalog-events
        group-id: catalog-service-group
        concurrency: 3
```

---

## Configuration

### How do I configure Kafka in Spring Middleware?
Configuration uses the `middleware.kafka` prefix.

**Complete Example (`application.yml`):**
```yaml
middleware:
  kafka:
    bootstrap-servers: localhost:9092
    create-missing-topics: true
    publishers:
      catalog:
        topic: catalog-events
    subscribers:
      catalog:
        topic: catalog-events
        group-id: catalog-service-group
        concurrency: 3
```

**Constraints:**
- The configuration prefix is `middleware.kafka`, **NOT** `spring.kafka`.
- `bootstrap-servers` is required for production.
- Named publishers MUST be declared under `middleware.kafka.publishers.<id>`.
- Subscribers MUST be declared under `middleware.kafka.subscribers.<id>`.

---

## Subscribing to Events

### How do I create a Kafka subscriber?
To create a subscriber to consume Kafka messages, use the `@MiddlewareKafkaListener` annotation and pass it the subscriber ID as the value. Your method must accept an `EventEnvelope<T>` parameter.

**Java code:**
```java
@Component
public class OrderSubscriber {
    @MiddlewareKafkaListener("order-events")
    public void handle(EventEnvelope<OrderEvent> envelope) {
        OrderEvent event = envelope.getPayload();
        // Process the event
    }
}
```

**YAML Configuration required:**
```yaml
middleware:
  kafka:
    subscribers:
      order-events:
        topic: orders-topic
        group-id: orders-consumer-group
        concurrency: 1
```

**Constraints:**
- The `@MiddlewareKafkaListener` does NOT accept topic or group parameters. Those must be configured in `application.yml` under `middleware.kafka.subscribers.<subscriber_id>`.
- The method must accept exactly one parameter of type `EventEnvelope<T>`.

---

## Publishing Events

### How do I publish a Kafka event?
To publish a Kafka event, retrieve the publisher from `KafkaPublisherRegistry` by its ID, and call `publish()` or `publishWithKey()`. Both will automatically wrap the payload in an `EventEnvelope<T>` and return a `CompletableFuture<PublishResult<T,K>>`.

**Java code:**
```java
@Service
public class OrderPublisherService {
    private final KafkaPublisher<OrderEvent, String> publisher;

    public OrderPublisherService(KafkaPublisherRegistry registry) {
        // "orders-publisher" matches the ID in YAML
        this.publisher = registry.getPublisher("orders-publisher");
    }

    public void sendOrder(OrderEvent order) {
        publisher.publish(order).thenAccept(result -> {
            System.out.println("Published to partition: " + result.getPartition());
        });
    }
}
```

**YAML Configuration required:**
```yaml
middleware:
  kafka:
    publishers:
      orders-publisher:
        topic: orders-topic
```

### How do I define a custom event type name?
By default, the publisher uses the class simple name for the `eventType`. You can override this using the `@EventType` annotation.

**Java code:**
```java
import io.github.spring.middleware.kafka.api.annotations.EventType;

@EventType("order.created.v1")
public class OrderEvent {
    private String orderId;
    // ...
}
```

---

## Error Handling & Dead Letter Topics

### How do I configure Kafka error handling and Dead Letter Topics?
Error handling, retries, and dead letter queues are configured under `middleware.kafka.error-handling`. When enabled, records that fail processing will be retried and finally sent to a DLT (Dead Letter Topic).

**YAML Configuration:**
```yaml
middleware:
  kafka:
    error-handling:
      enabled: true
      max-retries: 3
      retry-backoff-ms: 1000
      dead-letter:
        enabled: true
        suffix: .DLT
```

**Constraints:**
- Error handling applies to subscriber endpoints.
- By default, it is enabled, retries 3 times, with a 1000ms backoff, and suffixes the DLT with `.DLT`.

---

## Logging

### How do I configure Kafka payload logging?
You can control middleware-level Kafka logging using the `middleware.kafka.logging` properties.

**YAML Configuration:**
```yaml
middleware:
  kafka:
    logging:
      enabled: true
      log-payload: false
      log-headers: true
```

**Constraints:**
- Avoiding `log-payload: true` is recommended in production unless absolutely needed because payloads can be large or sensitive.

