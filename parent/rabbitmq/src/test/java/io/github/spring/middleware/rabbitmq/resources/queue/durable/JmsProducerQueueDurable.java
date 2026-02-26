package io.github.spring.middleware.rabbitmq.resources.queue.durable;


import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import io.github.spring.middleware.rabbitmq.resources.handler.ProducerHandler;
import org.apache.commons.pool2.ObjectPool;

@JmsProducer
@ProducerHandler
@JmsDestination(name = "queue-durable", destinationType = DestinationType.QUEUE, clazzSuffix = EnvironmentSuffix.class)
public class JmsProducerQueueDurable extends JmsProducerResource<TestingMessage> {

    public JmsProducerQueueDurable(String routingKey, ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(routingKey, connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }
}
