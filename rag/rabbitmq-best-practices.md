# RabbitMQ Best Practices (RAG-Friendly)

## Quick Answer

**What are the key rules for writing RabbitMQ handlers and listeners?**
- **Naming**: Ensure handlers end with `HandlerResource` (e.g., `OrderProcessingHandlerResource`). Error handlers must end in `ErrorHandler` or `ErrorHandlerResource`. Marker annotations must describe the intent (e.g., `@DbTxHandler`).
- **Lifecycle**: ALWAYS release resources (like DB Sessions) inside the `handleFinallyProcessingMessage()` block. Never leave them open.
- **Transactions**: Default to `@JmsConsumer(transacted = false)` unless broker-side atomicity strictly demands it.
- **Mapping**: Handlers and ErrorHandlers MUST define themselves via annotations and be linked via the custom Marker annotation.

**Java Example (Full Marker-Based Error Handler):**
```java
// 1. You define a marker
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SendToDLT {}

// 2. You annotate the Handler logic explicitly with that Marker
@JmsErrorHandler(value = SendToDLT.class)
public class DltErrorHandler<T> implements JmsResourceErrorHandler<T> {
    @Override
    public void handleError(ErrorHandlerContext<T> ctx) {
        // Safe logic to push to DLT
    }
}

// 3. You attach it to the consumer
@SendToDLT
@JmsConsumer
public class OrderConsumer extends JmsConsumerResource<Order> { ... }
```

**Constraints:**
- You can ONLY attach ONE `JmsHandler` to a defined resource class. If multiple apply, the factory throws an error entirely.
- You should NEVER perform heavy blocking I/O directly in `handleBeforeProcessingMessage(...)` as it runs synchronously inside the message-processing thread.

---

## Acknowledgment Modes

### When using `Session.CLIENT_ACKNOWLEDGE`, how is the message acknowledged?
When `transacted = false` and acknowledgment is `CLIENT_ACKNOWLEDGE`, the framework dynamically attaches a `DefaultJmsAcknowledgeListener` that executes `message.acknowledge()` after your `process()` successfully completes.

**Constraints:**
- If you override the listener or rely on deep manual tracking, you MUST call `.acknowledge()` manually. Forgotten ACKs lead to infinite redeliveries.
