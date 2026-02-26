package io.github.spring.middleware.rabbitmq.core.destination.type;

import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import jakarta.jms.Session;


public class DestinationTypeMessageDestinationCreatorFunctionParameters implements DestinationTypeFunctionParameters {

    private Session session;
    private JmsResourceDestination jmsResourceDestination;

    public DestinationTypeMessageDestinationCreatorFunctionParameters(Session session, JmsResourceDestination jmsResourceDestination) {
        this.session = session;
        this.jmsResourceDestination = jmsResourceDestination;
    }

    public Session getSession() {
        return session;
    }

    public JmsResourceDestination getJmsResourceDestination() {
        return jmsResourceDestination;
    }
}
