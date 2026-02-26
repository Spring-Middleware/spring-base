package io.github.spring.middleware.rabbitmq.core;

import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationNamer;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationType;
import jakarta.jms.Destination;


public class JmsResourceDestination {

    private JmsDestination jmsDestination;
    private Destination destination;

    public JmsResourceDestination(Destination destination, JmsDestination jmsDestination) {
        this.destination = destination;
        this.jmsDestination = jmsDestination;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public String getDestinationName() {
        return DestinationNamer.getDestinationSuffixName(jmsDestination);
    }

    public String getTopicId() {
        return jmsDestination.id();
    }

    public DestinationType getDestinationType() {
        return jmsDestination.destinationType();
    }

    public JmsDestination getJmsDestination() {

        return jmsDestination;
    }

}
