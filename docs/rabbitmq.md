# RabbitMQ Module

Overview
--------
This module provides an annotation-driven JMS-like framework for RabbitMQ. It supports producers, consumers, connection pooling, message converters, lifecycle management, and centralized error handling.

Highlights
----------
- Annotation-driven producers (`@JmsProducer`) and consumers (`@JmsConsumer`).
- `JmsFactory` to scan packages and create `JmsResources` (producers/consumers lifecycle).
- Connection pooling with Apache Commons Pool2 and pluggable connection/session configuration.
- Built-in JSON and XML converters; extensible `Converter<T>` implementations.
- Support for durable queues, topics, bindings, selectors, and error handlers.

Quick start
-----------
- Configure `JmsConnectionConfiguration` and credentials.
- Initialize `JmsResources` via `JmsFactory.newInstance().createJmsResources(...)` and provide the package to scan.
- Use the created producer instances to send messages, and start/stop consumers via the resources API.

Listener, handler and error handler patterns
-------------------------------------------
This module provides three complementary extension points that you can implement to observe and control message processing:

- `@JmsListener` — application-level listeners that are invoked before message processing. Use this to implement cross-cutting concerns (metrics, tracing, conditional short-circuit, etc.).
  - Implementation: `io.github.spring.middleware.rabbitmq.annotations.JmsListener` and listeners implement `io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener`.
  - Behavior: listeners are discovered by `JmsResourceFactory.createListeners(...)` and attached to matching resources. Listeners are executed in priority order via `JmsConsumerResource.processWithListeners(...)`.

- `@JmsHandler` — a single, pluggable handler component that wraps the processing flow and exposes lifecycle hooks (before/after/exception/finally). This is useful when you need transactional wrapper logic, custom conversion, or advanced error handling behavior for a consumer.
  - Implementation: `io.github.spring.middleware.rabbitmq.annotations.JmsHandler` and handler class must be compatible with `io.github.spring.middleware.rabbitmq.core.resource.handler.JmsHandlerResource`.
  - Behavior: `JmsResourceFactory.createHandlers(...)` wires a single handler per consumer class. The handler's `handle(...)` method is used by consumers to delegate processing and to implement `handleBeforeProcessingMessage`, `handleAfterProcessingMessage`, `handleExceptionProcessinggMessage` and `handleFinallyProcessingMessage` hooks.

- `@JmsErrorHandler` — error handler components discovered and invoked when message processing fails (after retries/rollback logic). Error handlers implement `io.github.spring.middleware.rabbitmq.core.resource.handler.JmsResourceErrorHandler`.
  - Implementation: `io.github.spring.middleware.rabbitmq.annotations.JmsErrorHandler` and the runtime type `io.github.spring.middleware.rabbitmq.core.resource.handler.JmsResourceErrorHandler`.
  - Behavior: `JmsResourceFactory.createErrorHandlers(...)` discovers and attaches error handlers to matching resources. The consumer calls `handleError(...)` to delegate to registered error handlers when processing fails.

Matching rules
--------------
`JmsResourceFactory` uses annotation-based matching to attach listeners, handlers and error handlers to resources:
- A listener/error-handler/handler annotated with `@JmsXxx(value = JmsAll.class)` is matched to all resources.
- If the target resource class is annotated with the same listener/handler `value` the component is attached.
- Convenience annotations `JmsAllConsumers` / `JmsAllProducers` allow scoping handlers/listeners to only consumers or only producers.

Important details about discovery and ordering
---------------------------------------------
- The same marker-based mechanism applies to `@JmsListener`, `@JmsHandler` and `@JmsErrorHandler`. The framework discovers components annotated with those framework annotations and attaches them to resource classes that carry the user-defined marker annotation referenced in the component's `value()` attribute.
- If a resource class is NOT annotated with a user marker annotation, it will NOT be matched by components that target that marker (unless the component uses `JmsAll` or other global scopes). In other words: the resource must be annotated with the marker to be recognized by that specific handler/listener/error-handler.
- Multiplicity rules:
  - `@JmsHandler`: only one handler may be attached to a given resource class; the factory logs an error if multiple handlers match the same class.
  - `@JmsListener` and `@JmsErrorHandler`: multiple listeners or error handlers may be attached to the same resource. When more than one applies, they are executed in order determined by their `priority()` attribute.
- Priority semantics: a larger `priority` numeric value means the component runs earlier (higher precedence). If two components have the same priority the framework's tie-breaker is implementation-dependent (typically discovery order); do not rely on undefined ties — set explicit priorities if ordering matters.

Transactions and acknowledgement modes
-------------------------------------
Consumers control session semantics using two configuration knobs provided by the `@JmsConsumer` annotation:

- `transacted` (boolean): when `true` the JMS `Session` used for the consumer is transacted. In that case the consumer code explicitly calls `session.commit()` after successful processing and `session.rollback()` when an exception occurs. See `io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource#onMessage` for the commit/rollback behavior.

- `acknoledgement` (int): the JMS acknowledge mode (the code uses the numeric constants from `jakarta.jms.Session`, e.g. `Session.CLIENT_ACKNOWLEDGE`). When `transacted == false` and `acknoledgement == Session.CLIENT_ACKNOWLEDGE` the factory will attach a `DefaultJmsAcknowledgeListener` to the consumer which calls `Message#acknowledge()` after successful processing.

Runtime behavior summary (consumer):

1. Message is delivered and `onMessage(Message)` is invoked in `JmsConsumerResource`.
2. The message properties and body are read and converted to the target type.
3. Registered `JmsResourceListener`s are executed in priority order.
4. The consumer processes the message (either directly or via a configured `JmsHandlerResource`).
5. If processing succeeds:
   - If the `Session` is transacted (`session.getTransacted()`), the consumer calls `session.commit()`.
   - Else if the session is non-transacted and the acknowledge mode is `Session.CLIENT_ACKNOWLEDGE` and an acknowledge listener is configured, the acknowledge listener will call `message.acknowledge()` (by default `DefaultJmsAcknowledgeListener` which delegates to `Message.acknowledge()`).
6. If processing throws an exception:
   - If the session is transacted the consumer attempts `session.rollback()`.
   - The consumer delegates to configured error handlers (`JmsResourceErrorHandler`) via `handleError(...)`.

Important notes and gotchas
--------------------------
- Transacted sessions ensure exactly-once processing semantics only if the broker and overall system configuration support it; they also change delivery/ack semantics and typically imply different visibility guarantees.
- `CLIENT_ACKNOWLEDGE` requires the application (or the provided `JmsAcknowledgeListener`) to explicitly acknowledge the message. If you forget to ack messages in this mode they may be redelivered.
- The module defaults to the values supplied by the `@JmsConsumer` annotation on the consumer class. Review those annotation defaults and set `transacted` and `acknoledgement` deliberately.

Where to look in the code
------------------------
- Listener/handler/error handler wiring: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/core/JmsResourceFactory.java`
- Consumer lifecycle and ack/tx logic: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/core/resource/consumer/JmsConsumerResource.java`
- Acknowledge listener interface and default: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/core/JmsAcknowledgeListener.java` and `DefaultJmsAcknowledgeListener.java`
- Handler resource class: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/core/resource/handler/JmsHandlerResource.java`
- Error handler interfaces: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/core/resource/handler/JmsResourceErrorHandler.java`
- Annotations: look under `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq/annotations` for `JmsListener`, `JmsHandler`, `JmsErrorHandler`, `JmsConsumer`, `JmsProducer`, `JmsDestination`.

Further reading
---------------
- Module README: `parent/rabbitmq/README.md` (contains examples and a simple integration test suite in `src/test`).
- Tests and example resources: `parent/rabbitmq/src/test/java/io/github/spring/middleware/rabbitmq/resources` show example consumer/producer classes wired using the annotations.
- Best practices: `docs/rabbitmq-best-practices.md` — recommended patterns for handlers, listeners and error handlers.

---

## Related documentation

- [README.md](../README.md)
- [Getting Started](./getting-started.md)
- [Architecture](./architecture.md)
- [Communication](./communication.md)
- [Errors](./errors.md)
- [Registry](./registry.md)
- [Kafka](./kafka.md)
- [Client Security](./client-security.md)
- [Logging](./logging.md)
- [Redis](./redis.md)
- [Mongo](./mongo.md)
- [JPA](./jpa.md)
- [Security](./security.md)
- [Core](./core.md)

## Examples & annotation mapping

Below are minimal examples (taken from the module tests) that show common usage patterns. These examples demonstrate how the module discovers and links user-defined annotations to the module annotations (`@JmsHandler`, `@JmsListener`, `@JmsErrorHandler`).

### Producer example

```java
@JmsProducer(bindings = {@JmsBinding(routingKey = "news.uk"), @JmsBinding(routingKey = "news.es")})
@JmsDestination(name = "amq.topic", exchange = "amq.topic", destinationType = DestinationType.TOPIC)
public class JmsProducerNewsWorld extends JmsProducerResource<TestingMessage> {

    public JmsProducerNewsWorld(String routingKey, ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(routingKey, connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }
}
```

- The producer is discovered by `JmsFactory` and created with the configured bindings and destination.

### Consumer example

```java
@JmsConsumer
@JmsDestination(name = "information-uk", destinationType = DestinationType.QUEUE)
public class JmsConsumerUK extends JmsConsumerResource<TestingMessage> {

    public void process(TestingMessage testingMessage, Properties properties) {
        logger.info("Message Received " + testingMessage.getMessage() + "-UK");
    }
}
```

- Consumers extend `JmsConsumerResource<T>` and implement `process(T message, Properties properties)`.
- The `@JmsConsumer` annotation controls `transacted` and `acknoledgement` options used when creating the `Session`.

### Handler example (annotation-driven wrapper)

User code defines a custom annotation and a handler resource. The module attaches the handler when a resource class is annotated with the custom annotation.

```java
// custom marker used by the handler
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumerHandler {}

// handler implementation wired with @JmsHandler
@JmsHandler(ConsumerHandler.class)
public class ConsumerHandlerResource<T> extends JmsHandlerResource<String, T> {
    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) throws JMSException {
        // e.g. open DB session, set MDC, etc.
        return "dbSession";
    }

    @Override
    public void handleFinallyConsumingMessage(String handlingMessage) {
        // cleanup
    }
}

// consumer annotated with the marker
@ConsumerHandler
@JmsConsumer
@JmsDestination(...)
public class MyConsumer extends JmsConsumerResource<MyDto> {
    public void process(MyDto dto, Properties properties) { ... }
}
```

- `JmsResourceFactory.createHandlers(...)` discovers classes annotated with `@JmsHandler` and attaches instances of `JmsHandlerResource` to consumer/producer resources whose class is annotated with the custom marker (here `@ConsumerHandler`).
- Only one handler per resource class is allowed (the factory logs an error if multiple handlers are configured for the same resource class).

### Listener example

Listeners are simple components executed before processing. They can target all resources or be scoped via a value annotation.

```java
@JmsListener
public class JmsListenerAll implements JmsResourceListener {
    @Override
    public void onBeforeProcessingMessage(Properties properties) {
        logger.info("[ALL] Listener on before process message");
    }
}
```

- `@JmsListener` without a value defaults to `JmsAll` and therefore will be attached to all resources.
- `JmsResourceFactory.createListeners(...)` adds discovered listeners to matching resources.

### Error handler example

Error handlers follow the same discovery/matching rules as listeners/handlers. The framework contains examples like `NotifyErrorHandler` in the project; an application-level implementation would look like:

```java
@Slf4j
@Component
@JmsErrorHandler(value = NotifyErrorHandler.class)
public class NotifyErrorHandlerResource<T> implements JmsResourceErrorHandler<T> {

    @Override
    public void handleError(ErrorHandlerContext<T> errorHandlerContext) {
        // send notifications, persist failure, push metrics, etc.
    }
}
```

- If a resource class is annotated with `@NotifyErrorHandler` (a custom marker annotation defined by the application), the module will attach the `NotifyErrorHandlerResource` to that resource.
- `@JmsErrorHandler(value = JmsAll.class)` would attach the error handler to all resources (both consumers and producers).

### Example: user marker annotation + error handler (DLT example)

```java
// 1) application-defined marker annotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DltError {}

// 2) application-provided error handler wired to the marker via @JmsErrorHandler
@JmsErrorHandler(value = DltError.class)
public class DltErrorHandler<T> implements JmsResourceErrorHandler<T> {
    @Override
    public void handleError(ErrorHandlerContext<T> ctx) {
        // example: route failed message to a Dead Letter Topic/Queue
        dltProducer.send(ctx.getMessage(), ctx.getProperties());
    }
}

// 3) any resource annotated with @DltError will be handled by DltErrorHandler
@DltError
@JmsConsumer
@JmsDestination(name = "orders", destinationType = DestinationType.QUEUE)
public class OrdersConsumer extends JmsConsumerResource<Order> {
    @Override
    public void process(Order order, Properties properties) { ... }
}
```

- The important piece is the `value` attribute in `@JmsErrorHandler` (or `@JmsHandler` / `@JmsListener`) which points to the user marker annotation.
- When the framework scans for handlers/listeners/error-handlers it will attach `DltErrorHandler` to any resource class annotated with `@DltError`.
- This same pattern works for `@JmsHandler` and `@JmsListener` (e.g. `@JmsHandler(MyMarker.class)` attaches the handler to resources annotated with `@MyMarker`).

### Annotation matching rules (summary)

- Module scans at startup via reflection for:
  - classes annotated with `@JmsHandler` — handler resources
  - classes annotated with `@JmsListener` — listeners
  - classes annotated with `@JmsErrorHandler` — error handlers
- The `value()` parameter in those annotations indicates the *user marker annotation* used to link components to resources. Example: `@JmsHandler(MyMarker.class)` means "attach this handler to resources annotated with `@MyMarker`".
- Special convenience values exist:
  - `JmsAll` — apply to all resources (both producers and consumers)
  - `JmsAllConsumers` — apply to all consumer resources
  - `JmsAllProducers` — apply to all producer resources
- Matching implementation details:
  - `JmsResourceFactory` checks three conditions to attach a component to a resource:
    1. `componentAnnotation.value().isAssignableFrom(JmsAll.class)` (component explicitly targets all)
    2. `mathClazzAnnotationCorresponding(resource, componentAnnotation.value())` (convenience consumer/producer scoping)
    3. `resource.getClass().isAnnotationPresent(componentAnnotation.value())` (resource is annotated with the user marker)

### Practical example: NotifyErrorHandler

- Define user marker annotation `@NotifyErrorHandler` on resource classes you want to observe.
- Implement a handler resource annotated with `@JmsErrorHandler(value = NotifyErrorHandler.class)` to provide the runtime error handling behavior.
- The module will automatically wire that implementation to any resource annotated with `@NotifyErrorHandler` at startup.
