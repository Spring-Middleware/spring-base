# RabbitMQ Java Queue Framework

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)](https://maven.apache.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.spring-middleware/bom.svg)](https://search.maven.org/artifact/io.github.spring-middleware/bom)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12+-orange.svg)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A lightweight, annotation-driven JMS framework for RabbitMQ that enables seamless integration of message producers and consumers in Java applications, independent of any specific framework (Spring, Jakarta EE, etc.).

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Core Concepts](#-core-concepts)
- [Configuration](#-configuration)
- [Usage Examples](#-usage-examples)
- [API Reference](#-api-reference)
- [Testing](#-testing)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [Published artifacts and installation](#-published-artifacts-and-installation)

## ✨ Features

- **Annotation-Driven**: Simple, declarative API using Java annotations
- **Framework-Independent**: Works with any Java application (Spring, Jakarta EE, plain Java)
- **Connection Pooling**: Built-in Apache Commons Pool2 integration for efficient connection management
- **Multiple Destination Types**: Support for both Queues and Topics
- **Durable & Transient Messaging**: Configure message persistence per destination
- **Multiple Consumer Instances**: Scale consumers horizontally with configurable instances
- **Routing Keys & Bindings**: Flexible routing with multiple binding support for producers
- **Message Converters**: Built-in JSON and XML converters with extensibility
- **Listener Pattern**: Pre/post processing hooks for producers and consumers
- **Error Handling**: Centralized error handling with custom error handlers
- **Message Selectors**: Filter messages based on custom criteria
- **Transaction Support**: Configurable transaction and acknowledgement modes

## 🏗️ Architecture

The framework follows a clean, modular architecture:

```
┌─────────────────────────────────────────────────────────┐
│                   Client Application                     │
│  (@JmsProducer / @JmsConsumer annotated classes)        │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                    JmsFactory                            │
│  • Scans annotations                                     │
│  • Creates JmsResources                                  │
│  • Configures connections                                │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  JmsResources                            │
│  • Manages producers and consumers                       │
│  • Lifecycle management (start/stop/close)               │
└─────────────┬───────────────┬───────────────────────────┘
              │               │
     ┌────────▼──────┐   ┌───▼──────────┐
     │  Producers     │   │  Consumers   │
     │  • Send msgs   │   │  • Receive   │
     │  • Handlers    │   │  • Process   │
     │  • Converters  │   │  • Listeners │
     └────────┬───────┘   └───┬──────────┘
              │               │
┌─────────────▼───────────────▼───────────────────────────┐
│            JmsConnectionManager                          │
│  • Connection pooling (Apache Commons Pool2)             │
│  • Retry logic                                           │
│  • Connection lifecycle                                  │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  RabbitMQ Broker                         │
│  • Exchanges, Queues, Bindings                           │
│  • Message routing and delivery                          │
└─────────────────────────────────────────────────────────┘
```

## 📦 Prerequisites

- **Java 8+**
- **Maven 3.x**
- **RabbitMQ 3.x+** (or AMQP-compatible broker)
- **Docker** (optional, for local RabbitMQ instance)

## 🚀 Quick Start

### 1. Add / Use the Module

If you consume this project as a published external artifact, add the library coordinates provided by the release or by your organization's artifact repository (check the BOM or the published release notes for exact groupId:artifactId:version). The README intentionally does not hard-code coordinates here to avoid stale data — prefer the coordinates from the release or the parent BOM.

If you are working inside the `spring-base` monorepo and build this module as part of the reactor (multi-module Maven build), you do NOT need to add an extra Maven dependency: the module is available directly on the build classpath when you run `mvn install` from the repository root.

Important: using the library at runtime does NOT require Docker or Testcontainers. Docker is required only to run the automated integration tests (Testcontainers) or to run the local example RabbitMQ via docker-compose (see next step).

### 2. Start RabbitMQ (Docker)

Optional — start RabbitMQ with Docker Compose for manual testing or local examples:

```bash
docker-compose up -d
```

This will expose:
- AMQP port: `5672`
- Management UI: `http://localhost:15672` (admin/admin)

Important: The project's automated integration tests use Testcontainers. To run `mvn test` locally you must have a working Docker engine available (Testcontainers launches RabbitMQ containers on demand). If you cannot run Docker locally, you can still build the project (`mvn -DskipTests package`) but integration tests that require a broker will be skipped or will fail.

### 3. Create a Producer

```java
@JmsProducer
@JmsDestination(
    name = "my-queue",
    destinationType = DestinationType.QUEUE,
    durable = true
)
public class MyProducer extends JmsProducerResource<MyMessage> {
    
    public MyProducer(String routingKey, 
                      ObjectPool<JmsConnection> connectionPool,
                      JmsSessionParameters sessionParams,
                      JmsResourceDestination destination,
                      Class<MyMessage> clazz) {
        super(routingKey, connectionPool, sessionParams, destination, clazz);
    }
}
```

### 4. Create a Consumer

```java
@JmsConsumer(instances = 2)
@JmsDestination(
    name = "my-queue",
    destinationType = DestinationType.QUEUE,
    durable = true
)
public class MyConsumer extends JmsConsumerResource<MyMessage> {
    
    public MyConsumer(ObjectPool<JmsConnection> connectionPool,
                      JmsSessionParameters sessionParams,
                      JmsResourceDestination destination,
                      Class<MyMessage> clazz) {
        super(connectionPool, sessionParams, destination, clazz);
    }
    
    @Override
    public void process(MyMessage message, Properties properties) {
        // Process your message here
        System.out.println("Received: " + message);
    }
}
```

### 5. Initialize and Use

```java
// Configure connection
JmsConnectionConfiguration config = new JmsConnectionConfiguration();
config.setTcpHost("tcp://localhost:5672");

JmsConnectionCredentials credentials = new JmsConnectionCredentials();
credentials.setUsername("admin");
credentials.setPassword("admin");
config.setJmsConnectionCredentials(credentials);

// Create resources
JmsFactory factory = JmsFactory.newInstance();
JmsResources resources = factory.createJmsResources(
    Arrays.asList("com.yourpackage.jms"), 
    config
);

// Get producer and send message
MyProducer producer = resources.getJmsProducer(MyProducer.class);
producer.send(new MyMessage("Hello, RabbitMQ!"));

// Start consumers
resources.start(MyConsumer.class);

// Cleanup
resources.close();
```

## 🎯 Core Concepts

### Annotations

#### `@JmsDestination`
Defines the destination configuration (queue/topic) for producers and consumers.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | String | "" | Destination name |
| `destinationType` | DestinationType | QUEUE | QUEUE or TOPIC |
| `exchange` | String | "amq.direct" | Exchange name |
| `schema` | String | "direct" | Exchange type (direct, topic, fanout) |
| `durable` | boolean | true | Message persistence |
| `id` | String | "" | Topic subscription ID (required for durable topics) |
| `clazzSuffix` | Class | DefaultDestinationSuffix | Custom suffix for destination naming |

#### `@JmsProducer`
Marks a class as a message producer.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `transacted` | boolean | false | Enable JMS transactions |
| `acknoledgement` | int | AUTO_ACKNOWLEDGE | Acknowledgement mode |
| `bindings` | JmsBinding[] | {@JmsBinding} | Multiple routing keys |

#### `@JmsConsumer`
Marks a class as a message consumer.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `transacted` | boolean | false | Enable JMS transactions |
| `acknoledgement` | int | AUTO_ACKNOWLEDGE | Acknowledgement mode |
| `instances` | int | 1 | Number of consumer instances |
| `selector` | Class | DefaultJmsSelector | Message selector implementation |

#### `@JmsListener`
Adds lifecycle listeners (before/after message processing).

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | Class | JmsAll | Target annotation (JmsAll, JmsAllProducers, JmsAllConsumers) |
| `priority` | int | 0 | Execution priority |

#### `@JmsHandler`
Adds message handlers (preprocessing/postprocessing logic).

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | Class | - | Target annotation (required) |

#### `@JmsErrorHandler`
Centralized error handling for message processing failures.

### Connection Pooling

The framework uses Apache Commons Pool2 for efficient connection management:

```java
JmsConnectionPoolConfiguration poolConfig = new JmsConnectionPoolConfiguration();
poolConfig.setMinIdle(1);      // Minimum idle connections
poolConfig.setMaxIdle(5);      // Maximum idle connections
poolConfig.setMaxTotal(10);    // Maximum total connections
config.setJmsConnectionPoolConfiguration(poolConfig);
```

### Message Converters

Built-in converters for JSON and XML:

- **JsonConverter**: Uses Jackson for JSON serialization/deserialization
  - Supports Java 8 date/time types
  - Configurable serialization features
  - Generic type support

- **XmlConverter**: Uses JAXB for XML marshalling/unmarshalling

Custom converters can be implemented via the `Converter<T>` interface.

## ⚙️ Configuration

### Connection Configuration

```java
JmsConnectionConfiguration config = new JmsConnectionConfiguration();
config.setTcpHost("tcp://broker.example.com:5672");

JmsConnectionCredentials credentials = new JmsConnectionCredentials();
credentials.setUsername("username");
credentials.setPassword("password");
config.setJmsConnectionCredentials(credentials);
```

### Session Configuration

Acknowledgement modes:
- `Session.AUTO_ACKNOWLEDGE`: Automatic acknowledgement
- `Session.CLIENT_ACKNOWLEDGE`: Manual acknowledgement
- `Session.DUPS_OK_ACKNOWLEDGE`: Lazy acknowledgement
- `Session.SESSION_TRANSACTED`: Transactional

## 📚 Usage Examples

### Queue Example (Durable)

**Producer:**
```java
@JmsProducer
@JmsDestination(
    name = "order-queue",
    destinationType = DestinationType.QUEUE,
    durable = true
)
public class OrderProducer extends JmsProducerResource<Order> {
    // Constructor...
}
```

**Consumer:**
```java
@JmsConsumer(instances = 4)
@JmsDestination(
    name = "order-queue",
    destinationType = DestinationType.QUEUE,
    durable = true
)
public class OrderConsumer extends JmsConsumerResource<Order> {
    
    @Override
    public void process(Order order, Properties properties) {
        // Process order
        log.info("Processing order: " + order.getId());
    }
}
```

### Topic Example (Pub/Sub)

**Producer:**
```java
@JmsProducer(bindings = {
    @JmsBinding(routingKey = "news.world"),
    @JmsBinding(routingKey = "news.sports")
})
@JmsDestination(
    name = "news",
    destinationType = DestinationType.TOPIC,
    exchange = "news-exchange",
    schema = "topic"
)
public class NewsProducer extends JmsProducerResource<NewsArticle> {
    // Constructor...
}
```

**Consumers:**
```java
// Consumer for all news
@JmsConsumer
@JmsDestination(
    name = "news",
    id = "all-news-consumer",
    destinationType = DestinationType.TOPIC,
    exchange = "news-exchange",
    schema = "topic"
)
public class AllNewsConsumer extends JmsConsumerResource<NewsArticle> {
    // Implementation...
}

// Consumer for world news only
@JmsConsumer
@JmsDestination(
    name = "news",
    id = "world-news-consumer",
    destinationType = DestinationType.TOPIC,
    exchange = "news-exchange",
    schema = "topic"
)
public class WorldNewsConsumer extends JmsConsumerResource<NewsArticle> {
    // Implementation with custom selector
}
```

### Message Handler Example

```java
@JmsHandler(value = MyProducerAnnotation.class)
public class MyHandler<T> extends JmsHandlerResource<String, T> {
    
    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) 
            throws JMSException {
        // Pre-processing logic
        properties.setProperty("TRANSACTION_ID", UUID.randomUUID().toString());
        return "Handler context";
    }
    
    @Override
    public void handleFinallyProcessingMessage(String context, T message, Properties properties) {
        // Post-processing cleanup
        log.debug("Completed with context: " + context);
    }
}
```

### Listener Example

```java
@JmsListener(value = JmsAllConsumers.class, priority = 10)
public class MessageListener extends JmsResourceListener {
    
    @Override
    public void beforeStart(JmsResource resource) {
        log.info("Starting consumer: " + resource.getClass().getName());
    }
    
    @Override
    public void afterClose(JmsResource resource) {
        log.info("Closed consumer: " + resource.getClass().getName());
    }
}
```

### Custom Message Selector

```java
public class EnvironmentSelector implements JmsSelector {
    
    @Override
    public boolean select() {
        String env = System.getProperty("environment");
        return "production".equals(env);
    }
}

@JmsConsumer(selector = EnvironmentSelector.class)
@JmsDestination(name = "my-queue")
public class MyConsumer extends JmsConsumerResource<MyMessage> {
    // Only processes messages when environment is "production"
}
```

## 📖 API Reference

### JmsFactory

Main entry point for creating JMS resources.

```java
JmsFactory factory = JmsFactory.newInstance();
JmsResources resources = factory.createJmsResources(
    List<String> packages,
    JmsConnectionConfiguration config
);
```

### JmsResources

Container for all producers and consumers.

**Key Methods:**
- `<T> T getJmsProducer(Class<T> clazz)` - Get producer by class
- `<T> T getJmsProducer(Class<T> clazz, String routingKey)` - Get producer with specific routing key
- `<T> T getJmsConsumer(Class<T> clazz)` - Get consumer by class
- `<T> List<T> getJmsConsumers(Class<T> clazz)` - Get all consumer instances
- `void start(Class<? extends JmsConsumerResource> clazz)` - Start consumers
- `void startAll()` - Start all consumers
- `void stop(Class<? extends JmsConsumerResource> clazz)` - Stop consumers
- `void close()` - Close all resources

### JmsProducerResource<T>

Base class for all producers.

**Key Methods:**
- `void send(T message)` - Send message with default converter
- `void send(T message, String mediaType)` - Send with specific converter
- `String getRoutingKey()` - Get current routing key

### JmsConsumerResource<T>

Base class for all consumers.

**Key Methods:**
- `abstract void process(T message, Properties properties)` - Process received message (must implement)
- `Integer getId()` - Get consumer instance ID

## 🧪 Testing

### Run All Tests

```bash
# Start RabbitMQ
docker-compose up -d

# Run tests
mvn test

# Shutdown
docker-compose down
```

### Test Structure

The project includes comprehensive tests demonstrating:
- **Queue messaging** (durable and transient)
- **Topic messaging** (pub/sub patterns)
- **Multiple consumer instances**
- **Message handlers and listeners**
- **Error handling**
- **Custom selectors**

See `src/test/java/com/middleware/jms/JmsTest.java` for complete examples.

## 📁 Project Structure

```
rabbitmq-java-queue/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/middleware/jms/
│   │           ├── JmsFactory.java                    # Main factory
│   │           ├── annotations/                       # Framework annotations
│   │           │   ├── JmsBinding.java
│   │           │   ├── JmsConsumer.java
│   │           │   ├── JmsDestination.java
│   │           │   ├── JmsDurability.java
│   │           │   ├── JmsErrorHandler.java
│   │           │   ├── JmsHandler.java
│   │           │   ├── JmsListener.java
│   │           │   ├── JmsProducer.java
│   │           │   ├── JmsQueue.java
│   │           │   ├── JmsTopic.java
│   │           │   └── listener/                      # Listener scopes
│   │           ├── configuration/                     # Connection config
│   │           │   ├── JmsConnectionConfiguration.java
│   │           │   ├── JmsConnectionCredentials.java
│   │           │   └── JmsConnectionPoolConfiguration.java
│   │           ├── connection/                        # Connection management
│   │           │   ├── JmsConnection.java
│   │           │   ├── JmsConnectionListener.java
│   │           │   ├── JmsConnectionManager.java
│   │           │   └─�� JmsConnectionPoolFactory.java
│   │           ├── converter/                         # Message converters
│   │           │   ├── Converter.java
│   │           │   ├── ConverterException.java
│   │           │   ├── ConverterFactory.java
│   │           │   ├── JsonConverter.java
│   │           │   └── XmlConverter.java
│   │           └── core/                              # Core framework
│   │               ├── DefaultJmsSelector.java
│   │               ├── JmsAcknowledgeListener.java
│   │               ├── JmsInitialContextFactory.java
│   │               ├── JmsResourceDestination.java
│   │               ├── JmsResourceFactory.java
│   │               ├── JmsResources.java
│   │               ├── JmsSelector.java
│   │               ├── JmsSelectorByHostname.java
│   │               ├── JmsSessionParameters.java
│   │               ├── destination/                   # Destination types
│   │               └── resource/                      # Resource abstractions
│   │                   ├── JmsResource.java
│   │                   ├── JmsResourceType.java
│   │                   ├── consumer/                  # Consumer infrastructure
│   │                   ├── handler/                   # Handler infrastructure
│   │                   ├── listener/                  # Listener infrastructure
│   │                   └── producer/                  # Producer infrastructure
│   └── test/
│       └── java/
│           └── com/middleware/jms/
│               ├── JmsTest.java                       # Main test suite
│               ├── message/
│               │   └── TestingMessage.java            # Test message DTO
│               └── resources/                         # Example implementations
│                   ├── queue/                         # Queue examples
│                   │   ├── durable/
│                   │   └── transients/
│                   ├── topic/                         # Topic examples
│                   ├── handler/                       # Handler examples
│                   └── listener/                      # Listener examples
├── docker-compose.yaml                                # RabbitMQ setup
├── definitions.json                                   # RabbitMQ config
└── pom.xml                                            # Maven config
```

## 🔑 Key Classes Reference

### Core Package (`com.middleware.jms.core`)

| Class | Purpose |
|-------|---------|
| `JmsResources` | Container managing all producers/consumers lifecycle |
| `JmsResourceFactory` | Factory creating producer/consumer instances |
| `JmsResourceDestination` | Destination metadata (queue/topic name, exchange) |
| `JmsSessionParameters` | Session configuration (transacted, acknowledgement) |
| `JmsSelector` | Interface for message filtering |
| `DefaultJmsSelector` | Default selector (accepts all messages) |
| `JmsSelectorByHostname` | Hostname-based message filtering |

### Connection Package (`com.middleware.jms.connection`)

| Class | Purpose |
|-------|---------|
| `JmsConnectionManager` | Manages connection lifecycle (create/start/close) |
| `JmsConnection` | Wrapper around JMS Connection with state tracking |
| `JmsConnectionPoolFactory` | Apache Commons Pool2 integration |
| `JmsConnectionListener` | Listener for connection events |

### Converter Package (`com.middleware.jms.converter`)

| Class | Purpose |
|-------|---------|
| `Converter<T>` | Interface for message serialization/deserialization |
| `JsonConverter<T>` | JSON converter using Jackson |
| `XmlConverter<T>` | XML converter using JAXB |
| `ConverterFactory` | Factory for creating converters |

### Resource Package (`com.middleware.jms.core.resource`)

| Class | Purpose |
|-------|---------|
| `JmsResource<T>` | Base class for all JMS resources |
| `JmsProducerResource<T>` | Base class for message producers |
| `JmsConsumerResource<T>` | Base class for message consumers |
| `JmsHandlerResource<C, T>` | Base class for message handlers |
| `JmsResourceListener` | Lifecycle hooks for resources |
| `JmsResourceErrorHandler<T>` | Error handling for message processing |

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone repository
git clone <repository-url>
cd rabbitmq-java-queue

# Start RabbitMQ
docker-compose up -d

# Build project
mvn clean install

# Run tests
mvn test
```

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built on top of [Apache Qpid](https://qpid.apache.org/) JMS client
- Uses [Jackson](https://github.com/FasterXML/jackson) for JSON processing
- Connection pooling via [Apache Commons Pool2](https://commons.apache.org/proper/commons-pool/)
- Annotation scanning via [Reflections](https://github.com/ronmamo/reflections)

## 📞 Support

For issues, questions, or contributions, please open an issue in the repository.

---

**Version:** 2.7  
**Java Version:** 8+  
**Last Updated:** January 2026

## Published artifacts and installation

This module is published to Maven Central under the `io.github.spring-middleware` group. You can consume it in two ways:

1) Preferred: import the project's BOM to manage versions centrally (recommended):

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.spring-middleware</groupId>
      <artifactId>bom</artifactId>
      <version>REPLACE_WITH_BOM_VERSION</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then add the module dependency without a version (the BOM will provide it):

```xml
<dependency>
  <groupId>io.github.spring-middleware</groupId>
  <artifactId>rabbitmq-java-queues</artifactId>
  <!-- version managed by the BOM -->
</dependency>
```

2) Direct dependency (single artifact):

Maven:

```xml
<dependency>
  <groupId>io.github.spring-middleware</groupId>
  <artifactId>rabbitmq-java-queues</artifactId>
  <version>REPLACE_WITH_VERSION</version>
</dependency>
```

Gradle (Groovy DSL):

```groovy
implementation 'io.github.spring-middleware:rabbitmq-java-queues:REPLACE_WITH_VERSION'
```

Notes:
- If you are developing inside the `spring-base` monorepo and build this module locally as part of the reactor, you do not need to add the dependency — the module will be on the build classpath.
- Docker / Testcontainers are required only to run the module's integration tests locally (or to start a local RabbitMQ via `docker-compose` for manual testing); they are not required to depend on or run the library in production.

See the published BOM and artifact versions on Maven Central: https://search.maven.org/artifact/io.github.spring-middleware/bom
