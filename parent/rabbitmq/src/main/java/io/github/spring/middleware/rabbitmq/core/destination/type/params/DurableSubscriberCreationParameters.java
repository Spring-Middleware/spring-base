package io.github.spring.middleware.rabbitmq.core.destination.type.params;

import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import jakarta.jms.Session;
import jakarta.jms.Topic;


public class DurableSubscriberCreationParameters extends MessageDestinationCreationParameters<Topic> {

    public DurableSubscriberCreationParameters(JmsResourceDestination jmsResourceDestination, Session session) {
        super(jmsResourceDestination, session);
    }

    public String getTopicId() {
        return getJmsResourceDestination().getTopicId();
    }
}
