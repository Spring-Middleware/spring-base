package io.github.spring.middleware.rabbitmq.core.destination.type.params;

import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import jakarta.jms.Queue;
import jakarta.jms.Session;


public class QueueDestinationCreationParameters extends MessageDestinationCreationParameters<Queue> {

    public QueueDestinationCreationParameters(JmsResourceDestination jmsResourceDestination, Session session) {
        super(jmsResourceDestination, session);
    }
}
