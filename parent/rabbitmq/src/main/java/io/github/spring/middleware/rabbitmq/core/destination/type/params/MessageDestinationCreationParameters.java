package io.github.spring.middleware.rabbitmq.core.destination.type.params;

import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionResult;
import jakarta.jms.Destination;
import jakarta.jms.Session;


public abstract class MessageDestinationCreationParameters<T extends Destination> implements DestinationTypeFunctionResult {

    private Session session;
    private JmsResourceDestination jmsResourceDestination;

    public MessageDestinationCreationParameters(JmsResourceDestination jmsResourceDestination, Session session) {
        this.jmsResourceDestination = jmsResourceDestination;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public JmsResourceDestination getJmsResourceDestination() {
        return jmsResourceDestination;
    }

    public void setJmsResourceDestination(JmsResourceDestination jmsResourceDestination) {
        this.jmsResourceDestination = jmsResourceDestination;
    }

    public T getDestination() {
        return (T) jmsResourceDestination.getDestination();
    }
}
