package io.github.spring.middleware.rabbitmq.resources.topic;


import io.github.spring.middleware.rabbitmq.annotations.JmsBinding;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import org.apache.commons.pool2.ObjectPool;

@JmsProducer(bindings = {@JmsBinding(routingKey = "news.uk", destinationQueue = "information-uk"), @JmsBinding(routingKey = "news.es", destinationQueue = "information-es")})
@JmsDestination(name = "amq.topic", schema = "topic", exchange = "amq.topic", destinationType = DestinationType.TOPIC)
public class JmsProducerNewsWorld extends JmsProducerResource<TestingMessage> {

    public JmsProducerNewsWorld(String routingKey, ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(routingKey, connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }
}
