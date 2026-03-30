# Kafka Integration

This document describes how to configure and use the Kafka support included in Spring Middleware.

Overview
--------
Spring Middleware provides an opinionated Kafka integration for publishers and subscribers. The module exposes an auto-configuration that creates producers, registers named publishers from properties, and can register consumer endpoints for configured subscribers.

The implementation lives under the `io.github.spring.middleware.kafka` packages (examples: `KafkaProperties`, `KafkaAutoConfiguration`, `KafkaPublisherRegistry`, `DefaultKafkaPublisher`, `KafkaListenerRegistrar`).

Configuration
-------------
The configuration prefix used by the module is `middleware.kafka` (not `spring.middleware.kafka`). Configure with YAML, properties, or environment variables.

Recommended application.yml example (aligned with the module):

```yaml
middleware:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    create-missing-topics: true
    logging:
      enabled: false        # enable middleware-level Kafka logging
      log-payload: false    # log message payloads (may be large)
      log-headers: false    # log headers
    topics:
      catalog-events:
        partitions: 5
        replication-factor: 3
    publishers:
      catalog:
        topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
    subscribers:
      catalog:
        topic: ${KAFKA_TOPIC_CATALOG:catalog-events}
        group-id: ${KAFKA_GROUP_ID_CATALOG:catalog-service-group}
        concurrency: 3
```

Error handling
--------------
The Kafka module supports configurable error handling for consumers (and related retry/dead-letter behaviour). The configuration keys are available under `middleware.kafka.error-handling` and are mapped in `KafkaProperties` (see the module sources for the exact mapping).

Example YAML snippet:

```yaml
middleware:
  kafka:
    error-handling:
      enabled: ${KAFKA_ERROR_HANDLING_ENABLED:true}
      max-retries: ${KAFKA_ERROR_HANDLING_MAX_RETRIES:3}
      retry-backoff-ms: ${KAFKA_ERROR_HANDLING_RETRY_BACKOFF_MS:1000}
      dead-letter:
        enabled: ${KAFKA_ERROR_HANDLING_DEAD_LETTER_ENABLED:true}
        suffix: .DLT
```

What these settings control (summary):
- `enabled` (boolean): enable/disable the module-level error handling behaviour.
- `max-retries` (int): number of retry attempts before considering the record failed.
- `retry-backoff-ms` (long): backoff (in milliseconds) between retries.
- `dead-letter.enabled` (boolean): when true, records that exhaust retries will be routed to a dead-letter topic.
- `dead-letter.suffix` (String): suffix appended to the original topic name to build the dead-letter topic name (e.g. `catalog-events.DLT`).

Flat `application.properties` version for the same configuration:

```
middleware.kafka.error-handling.enabled=${KAFKA_ERROR_HANDLING_ENABLED:true}
middleware.kafka.error-handling.max-retries=${KAFKA_ERROR_HANDLING_MAX_RETRIES:3}
middleware.kafka.error-handling.retry-backoff-ms=${KAFKA_ERROR_HANDLING_RETRY_BACKOFF_MS:1000}
middleware.kafka.error-handling.dead-letter.enabled=${KAFKA_ERROR_HANDLING_DEAD_LETTER_ENABLED:true}
middleware.kafka.error-handling.dead-letter.suffix=.DLT
```

Important property keys and defaults
-----------------------------------
- `middleware.kafka.bootstrap-servers` (String) — broker list (required for production; default usually `localhost:9092` in examples)
- `middleware.kafka.create-missing-topics` (boolean) — when true the auto-configuration will attempt to create the topics declared under `topics` (requires broker ACLs/permissions)
- `middleware.kafka.logging.*` — middleware logging controls
- `middleware.kafka.topics.<name>.partitions` (int) — topic partitions (default 1)
- `middleware.kafka.topics.<name>.replication-factor` (int) — replication factor (default 1)
- `middleware.kafka.publishers.<id>.topic` (String) — topic used by named publisher `<id>` (required for that publisher)
- `middleware.kafka.subscribers.<id>.topic` (String), `group-id`, `concurrency` — subscriber endpoint metadata wired by the registrar
- `middleware.kafka.error-handling.*` — error handling controls (`enabled`, `max-retries`, `retry-backoff-ms`, `dead-letter.*`) — defaults are shown in the YAML snippet above and are implemented in `KafkaProperties`.

How it wires
------------
- Publishers: the auto-configuration instantiates a `DefaultKafkaPublisher` for each entry under `middleware.kafka.publishers` and registers it in `KafkaPublisherRegistry` under the configured id.
- Subscribers: entries under `middleware.kafka.subscribers` are used by `KafkaListenerRegistrar` to register consumer endpoints (topic, group-id, concurrency). The registrar uses a message converter to convert incoming records into `EventEnvelope<T>` instances.

The error handling options are consumed by the module and will influence the retry behaviour and dead-letter routing for consumer endpoints that the registrar registers. See `parent/kafka/core/src/main/java/.../properties/KafkaProperties.java` for the authoritative property-to-field mapping and defaults.

API surface and key types
-------------------------
- `io.github.spring.middleware.kafka.core.registry.KafkaPublisherRegistry` — registry where publishers are registered and can be looked up by id.
- `io.github.spring.middleware.kafka.core.publisher.DefaultKafkaPublisher` — default implementation that builds an `EventEnvelope<T>` and sends it with a `KafkaTemplate`.
- `io.github.spring.middleware.kafka.api.publisher.KafkaPublisher<T,K>` — interface (publish / publishWithKey returning CompletableFuture<PublishResult<T,K>>).
- `io.github.spring.middleware.kafka.api.data.EventEnvelope<T>` — wrapper sent to Kafka; typical fields: `eventId`, `eventType`, `timestamp`, `traceId`, `payload`.
- `io.github.spring.middleware.kafka.api.data.PublishResult<T,K>` — publishing result (topic/partition/offset + original event info).
- `io.github.spring.middleware.kafka.api.annotations.EventType` — optional annotation to override the event type string used in the envelope.
- `io.github.spring.middleware.kafka.api.annotations.MiddlewareKafkaListener` — annotation used by the registrar to register typed listener methods.

Usage examples
--------------
1) Publishing via the registry

```text
- Inject or obtain the `KafkaPublisherRegistry` from your application context.
- Retrieve a publisher by id: `KafkaPublisher<MyEvent, String> publisher = publisherRegistry.getPublisher("catalog")`.
- Call `publish(event)` or `publishWithKey(event, key)`; both return a `CompletableFuture<PublishResult>`.
- Handle the `PublishResult` asynchronously to inspect topic/partition/offset.
```

Notes:
- The publisher wraps the payload into an `EventEnvelope<MyEvent>` automatically — you should implement your consumer to expect `EventEnvelope<T>`.
- If no publisher with the requested id exists or its topic is blank, the publisher will throw a `KafkaException`.

2) Declaring an event type (optional)

```text
import io.github.spring.middleware.kafka.api.annotations.EventType;

@EventType("order.created.v1")
public class OrderCreatedEvent {
    private String orderId;
    // ...
}
```

If the annotation is not present the publisher will default to the event class simple name for `eventType`.

3) Consuming events using the middleware registrar

The middleware provides `@MiddlewareKafkaListener` which can be applied to methods that receive `EventEnvelope<T>`:

```text
@MiddlewareKafkaListener("catalog") // matches middleware.kafka.subscribers.catalog config
public void onCatalogEvent(EventEnvelope<CatalogEvent> envelope) {
    CatalogEvent payload = envelope.getPayload();
    // handle payload
}
```

The registrar handles wiring topic, group id and concurrency according to the `middleware.kafka.subscribers` entry.

Event envelope shape
--------------------
An `EventEnvelope<T>` produced by `DefaultKafkaPublisher` contains at least:
- `eventId` (UUID)
- `eventType` (from `@EventType` or class simple name)
- `timestamp` (epoch millis)
- `traceId` (from MDC if available or generated)
- `payload` (the original event object)

PublishResult
-------------
`PublishResult<T,K>` is returned via the publisher's CompletableFuture and typically includes topic, partition and offset as well as the original envelope or event.

Best practices
--------------
- Declare topics in `middleware.kafka.topics` with partitions and replication factor for predictable creation when `create-missing-topics` is enabled.
- Use environment variables for production values (bootstrap servers, topic names, group ids) and fallbacks for local/dev.
- Use consistent publisher ids across services (e.g. `order-created`, `catalog`) so tooling and observability correlate events easily.
- Avoid logging full payloads in production (`middleware.kafka.logging.log-payload=false`) unless needed; payloads can be large or sensitive.

Where to look in the codebase
----------------------------
Key classes (examples of where to find the implementation):

- `parent/kafka/core/src/main/java/.../properties/KafkaProperties.java`
- `parent/kafka/core/src/main/java/.../autoconfigure/KafkaAutoConfiguration.java`
- `parent/kafka/core/src/main/java/.../registry/KafkaPublisherRegistry.java`
- `parent/kafka/core/src/main/java/.../publisher/DefaultKafkaPublisher.java`
- `parent/kafka/api/src/main/java/.../data/EventEnvelope.java`
- `parent/kafka/api/src/main/java/.../annotations/EventType.java`
- `parent/kafka/api/src/main/java/.../annotations/MiddlewareKafkaListener.java`

(Use your IDE to navigate the exact package names if needed.)

Further reading
---------------
See the middleware Kafka module source for advanced configuration and extension points (custom converters, custom KafkaTemplate beans, error handlers).

---

## Related documentation

- [README.md](../README.md)
- [Getting Started](./getting-started.md)
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [Registry](./registry.md)
- [Client Security](./client-security.md)
- [Logging](./logging.md)
- [RabbitMQ](./rabbitmq.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [JPA](./jpa.md)
- [Security](./security.md)
- [Core](./core.md)
