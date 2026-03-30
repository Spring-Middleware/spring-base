# RabbitMQ Module — Best Practices for Handlers, Listeners and Error Handlers

This short guide describes recommended patterns and conventions when implementing `JmsHandlerResource`, `JmsResourceListener` and `JmsResourceErrorHandler` in applications that use the RabbitMQ module.

Goal
----
Provide small, actionable guidance for naming, lifecycle management, priority and how to keep handlers/listeners/error handlers robust and maintainable.

1) Naming
---------
- Marker annotations (user-defined) should be named with a clear intent, e.g. `@OrderProcessingHandler`, `@NotifyErrorHandler`.
- Handler resource classes should end with `HandlerResource` (e.g. `OrderProcessingHandlerResource`) so they are easy to find.
- Listener classes should follow `*Listener` (e.g. `AuditListener`, `MetricsListener`).
- Error handler classes should end with `ErrorHandlerResource` or `ErrorHandler`.

2) Single responsibility
------------------------
- Keep handlers focused: a handler should either implement transactional/context wrapper logic or message transformation, not both.
- If you need both, prefer composing small handler resources rather than a single giant handler.

3) Lifecycle and resource management
-----------------------------------
- Always release resources in `handleFinallyProcessingMessage(...)`. Handlers commonly open DB sessions or external resources in `handleBeforeProcessingMessage` — ensure those are closed in the finally hook.
- Avoid blocking operations in `handleBeforeProcessingMessage(...)`: handlers run in the message processing thread; heavy blocking will reduce throughput. If you must call I/O, use a short-lived connection or an async pattern with timeouts.

4) Transactions and commit/rollback
-----------------------------------
- Prefer transacted sessions (`@JmsConsumer(transacted = true)`) only when the downstream resource supports the same transactional guarantees or when you need broker-side atomicity. Transacted sessions may increase complexity and latency.
- For `CLIENT_ACKNOWLEDGE`, ensure your handler calls acknowledge explicitly or let the configured `JmsAcknowledgeListener` handle it.

5) Priority and ordering
------------------------
- Use `@JmsListener(priority = X)` to define ordering for listeners. Higher numeric values run earlier.
- Keep the number of global listeners small — each listener adds overhead to processing.

6) Error handling strategy
--------------------------
- Prefer centralized error handlers for cross-cutting concerns (alerts, metrics, persistence of failed messages).
- Implement idempotency in consumers (or at the application level) to recover from redeliveries.
- Use error handlers to send failed events to a Dead Letter Topic/Queue when retries are exhausted.

7) Observability
-----------------
- Add structured logging in handlers and error handlers; include `requestId` / `traceId` from message properties or MDC.
- Export metrics for:
  - processing time
  - success/failure counts
  - retry attempts

8) Testing
----------
- Unit test handlers by invoking `handleBeforeProcessingMessage`, `handleFinallyProcessingMessage` and `handleExceptionProcessinggMessage` with a sample message and properties.
- Use integration tests with Testcontainers to validate end-to-end behavior (retry, dead-letter, ack/transacted semantics).

9) Examples (patterns)
----------------------
- Handler that opens a DB transaction:

```java
public class TxHandlerResource<T> extends JmsHandlerResource<DbSession, T> {
    @Override
    public DbSession handleBeforeProcessingMessage(T message, Properties properties) {
        return db.openSession();
    }

    @Override
    public void handleFinallyProcessingMessage(DbSession session, T message, Properties properties) {
        session.close();
    }
}
```

- Error handler that routes failed messages to a DLT topic:

```java
public class DltErrorHandler<T> implements JmsResourceErrorHandler<T> {
    @Override
    public void handleError(ErrorHandlerContext<T> ctx) {
        dltProducer.send(ctx.getMessage(), ctx.getProperties());
    }
}
```

10) Common pitfalls
-------------------
- Forgetting to close DB sessions in finally hooks.
- Performing long-running blocking operations in listeners or handlers without timeouts.
- Attaching multiple handlers to the same resource — the factory allows only one and logs an error.

11) Marker-based handlers and error handlers (explicit example)
----------------------------------------------------------------
The framework discovers handler and error-handler components when they are annotated with the framework annotations (`@JmsHandler`, `@JmsErrorHandler`) and the framework will attach them to resources annotated with the user marker annotation specified in the `value` attribute.

Important: the handler implementation itself must be annotated with `@JmsHandler(MyMarker.class)` (or `@JmsErrorHandler(MyMarker.class)`) for the factory to discover it.

Example (Dead Letter routing):

```java
// 1) User-defined marker annotation placed on resource classes
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DltError {}

// 2) Error handler implementation discovered by the framework
// The framework will attach this handler to any resource class annotated with @DltError
@JmsErrorHandler(value = DltError.class)
public class DltErrorHandler<T> implements JmsResourceErrorHandler<T> {
    @Override
    public void handleError(ErrorHandlerContext<T> ctx) {
        // route failed message to a Dead Letter Topic/Queue
        dltProducer.send(ctx.getMessage(), ctx.getProperties());
    }
}

// 3) Handler example (transactional wrapper) discovered the same way
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumerHandler {}

@JmsHandler(ConsumerHandler.class)
public class ConsumerHandlerResource<T> extends JmsHandlerResource<String, T> {
    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) throws JMSException {
        // open DB transaction or set MDC
        return "dbSession";
    }

    @Override
    public void handleFinallyConsumingMessage(String handlingMessage) {
        // commit/close session
    }
}

// 4) Usage on a resource class
@DltError
@ConsumerHandler
@JmsConsumer
@JmsDestination(name = "orders", destinationType = DestinationType.QUEUE)
public class OrdersConsumer extends JmsConsumerResource<Order> {
    @Override
    public void process(Order order, Properties properties) { /* ... */ }
}
```

Key points:
- `@JmsHandler`, `@JmsListener` and `@JmsErrorHandler` make the component discoverable. Their `value` attribute points to the *user marker annotation* used on resource classes.
- You can safely implement multiple different handler/error-handler types and attach them selectively using different marker annotations.
- Only one `JmsHandler` may be attached to a given resource class; the factory will log if multiple handlers match the same resource.

Further recommendations
-----------------------
- Keep handlers and listeners small and well-named so other developers can understand the intent quickly.
- Prefer configuration-driven behavior (properties) for retry counts and DLT suffixes to make production tuning easier.

<!-- no related documentation block required here; navigation links are provided in module README -->
