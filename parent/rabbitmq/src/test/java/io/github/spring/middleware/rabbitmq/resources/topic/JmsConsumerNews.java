package io.github.spring.middleware.rabbitmq.resources.topic;




import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@JmsConsumer
@JmsDestination(name = "news", destinationType = DestinationType.QUEUE)
public class JmsConsumerNews extends JmsConsumerResource<TestingMessage> {

    private Logger logger = LoggerFactory.getLogger(JmsConsumerNews.class);
    private String messageReceived = null;
    private int totalReceived = 0;

    public JmsConsumerNews(ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters, JmsResourceDestination jmsResourceDestination, Class<TestingMessage> clazz) {
        super(connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }

    public void process(TestingMessage testingMessage, Properties properties) {
        logger.info("Message Received " + testingMessage.getMessage() + "-" + "NEWS");
        messageReceived = testingMessage.getMessage();
        totalReceived++;
    }

    public void waitUntilMessageReceived(int waitFor) throws Exception {
        while (waitFor != totalReceived) {
            Thread.sleep(300);
        }
    }

}