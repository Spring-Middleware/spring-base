package io.github.spring.middleware.rabbitmq.resources.queue.transients;



import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import io.github.spring.middleware.rabbitmq.resources.handler.ConsumerHandler;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@JmsConsumer(instances = 2)
@ConsumerHandler
@JmsDestination(name = "queue-transient", destinationType = DestinationType.QUEUE, durable = false)
public class JmsConsumerQueueTransient extends JmsConsumerResource<TestingMessage> {

    private Logger logger = LoggerFactory.getLogger(JmsConsumerQueueTransient.class);
    private AtomicInteger atomicInteger;

    public JmsConsumerQueueTransient(ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }

    public void setAtomicInteger(AtomicInteger atomicInteger) {
        this.atomicInteger = atomicInteger;
    }

    public void process(TestingMessage testingMessage, Properties properties) {
        logger.info("Message Received " + testingMessage.getId() + " in consumer " + getId());
        atomicInteger.getAndIncrement();
    }

    public void waitUntilMessageReceived(Integer expectedMessages) throws Exception {
        while (atomicInteger.get() < expectedMessages) {
            Thread.sleep(300);
        }
    }


}
