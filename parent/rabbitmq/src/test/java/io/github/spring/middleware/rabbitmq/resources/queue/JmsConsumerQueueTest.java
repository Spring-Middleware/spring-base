package io.github.spring.middleware.rabbitmq.resources.queue;



import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import io.github.spring.middleware.rabbitmq.resources.listener.DurableConsumerListener;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@JmsConsumer(instances = 4)
@DurableConsumerListener
@JmsDestination(name = "queue-test", destinationType = DestinationType.QUEUE, clazzSuffix = EnvironmentSuffix.class)
public class JmsConsumerQueueTest extends JmsConsumerResource<TestingMessage> {

    private Logger logger = LoggerFactory.getLogger(JmsConsumerQueueTest.class);
    private AtomicInteger atomicInteger;

    public JmsConsumerQueueTest(ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
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
