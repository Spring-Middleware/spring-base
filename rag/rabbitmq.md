# RabbitMQ Module (RAG-Friendly)

## Quick Answer

**How do I publish a RabbitMQ message?**
Extend `JmsProducerResource` and use the `@JmsProducer` and `@JmsDestination` annotations.

**Java code:**
```java
import io.github.spring.middleware.rabbitmq.annotations.JmsBinding;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;

@JmsProducer(bindings = {@JmsBinding(routingKey = "news.uk"), @JmsBinding(routingKey = "news.es")})
@JmsDestination(name = "amq.topic", exchange = "amq.topic", destinationType = DestinationType.TOPIC)
public class JmsProducerNewsWorld extends JmsProducerResource<TestingMessage> {

    public JmsProducerNewsWorld(String routingKey, ObjectPool<JmsConnection> connectionPool, JmsSessionParameters params, JmsResourceDestination destination, Class<TestingMessage> clazz) {
        super(routingKey, connectionPool, params, destination, clazz);
    }
}
```

**How do I create a RabbitMQ consumer?**
Extend `JmsConsumerResource` and use the `@JmsConsumer` and `@JmsDestination` annotations.

**Java code:**
```java
import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;

@JmsConsumer
@JmsDestination(name = "information-uk", destinationType = DestinationType.QUEUE)
public class JmsConsumerUK extends JmsConsumerResource<TestingMessage> {

    @Override
    public void process(TestingMessage testingMessage, Properties properties) {
        System.out.println("Message Received: " + testingMessage.getMessage());
    }
}
```

**Constraints:**
- Consumers must extend `JmsConsumerResource<T>`.
- Producers must extend `JmsProducerResource<T>`.
- The factory uses reflection on packages defined by `JmsFactory.newInstance().createJmsResources(...)`.

---

## Listeners and Handlers

### How do I run code before processing all messages?
Implement `JmsResourceListener` and annotate the class with `@JmsListener`.

**Java code:**
```java
@JmsListener
public class LoggingListener implements JmsResourceListener {
    @Override
    public void onBeforeProcessingMessage(Properties properties) {
         // Executed before any matching consumer processes a message
         System.out.println("About to process a message");
    }
}
```

**Constraints:**
- `@JmsListener` by default applies to `JmsAll` (all resources).
- Listener execution order is determined by their `priority()` annotation parameter.

### How do I wrap a consumer with a handler (e.g., for opening a Session/DB Transaction)?
Use `@JmsHandler` tied to a custom user annotation.

**Java code:**
```java
// 1. Define the custom marker annotation
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbTxHandler {}

// 2. Create the Handler class
@JmsHandler(DbTxHandler.class)
public class DbTxHandlerResource<T> extends JmsHandlerResource<String, T> {
    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) {
        return "transactionId-123";
    }

    @Override
    public void handleFinallyConsumingMessage(String transactionId) {
        // cleanup transaction
    }
}

// 3. Mark your consumer
@DbTxHandler
@JmsConsumer
@JmsDestination(name = "orders", destinationType = DestinationType.QUEUE)
public class OrderConsumer extends JmsConsumerResource<OrderDto> {
    public void process(OrderDto dto, Properties properties) { ... }
}
```

**Constraints:**
- ONLY ONE handler may be attached to a given resource class. The factory throws an error if multiple match.
- This handler acts as a wrapper `try/catch/finally` block.

---

## Error Handling and Resiliency

### How do I configure a custom Error Handler or Dead Letter Queue?
Create a `JmsResourceErrorHandler` and annotate it with `@JmsErrorHandler`.

**Java code:**
```java
// 1. App-defined error marker
@Retention(RetentionPolicy.RUNTIME)
public @interface SendToDlt {}

// 2. The Error Handler itself
@JmsErrorHandler(value = SendToDlt.class)
public class DltErrorHandler<T> implements JmsResourceErrorHandler<T> {
    @Override
    public void handleError(ErrorHandlerContext<T> ctx) {
        System.err.println("Sending failed message to DLT");
        // Access ctx.getMessage() and ctx.getProperties()
    }
}

// 3. Any consumer that throws an error routes to DLT
@SendToDlt
@JmsConsumer
@JmsDestination(name = "payments", destinationType = DestinationType.QUEUE)
public class PaymentConsumer extends JmsConsumerResource<Payment> {
    // ...
}
```

**Constraints:**
- Error Handlers are invoked AFTER the consumer processes the message and throws an unhandled exception.
- Multiple error handlers can run in sequential order via `priority()`.

---

## Transactions & Acknowledgement modes

### How do I configure consumer session commits vs rollback?
Configure the `transacted` and `acknoledgement` booleans directly on the `@JmsConsumer` annotation.

**Java logic:**
- `transacted=true`: The consumer will automatically execute `session.commit()` if `process()` finishes cleanly. If `process()` throws, it attempts `session.rollback()`.
- `transacted=false` and `CLIENT_ACKNOWLEDGE`: A `DefaultJmsAcknowledgeListener` handles `message.acknowledge()`.
- The application code itself must acknowledge it manually if relying outside of the automatic behavior.
