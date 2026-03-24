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

Where to look
-------------
- Full module README: `parent/rabbitmq/README.md` (contains detailed examples, annotations and API reference).
- Core code: `parent/rabbitmq/src/main/java/io/github/spring/middleware/rabbitmq`.

Further reading
---------------
The module README is comprehensive; open `parent/rabbitmq/README.md` for full examples and API details.
