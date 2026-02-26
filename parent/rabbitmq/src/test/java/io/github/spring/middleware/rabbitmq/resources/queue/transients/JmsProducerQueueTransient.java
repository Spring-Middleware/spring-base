package io.github.spring.middleware.rabbitmq.resources.queue.transients;


import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import org.apache.commons.pool2.ObjectPool;

@JmsProducer
@JmsDestination(name = "queue-transient", destinationType = DestinationType.QUEUE, durable = false)
public class JmsProducerQueueTransient extends JmsProducerResource<TestingMessage> {

    public JmsProducerQueueTransient(String routingKey, ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(routingKey, connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }
}
