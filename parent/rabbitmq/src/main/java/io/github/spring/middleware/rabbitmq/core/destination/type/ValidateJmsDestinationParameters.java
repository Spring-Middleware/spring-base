package io.github.spring.middleware.rabbitmq.core.destination.type;

import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;

public class ValidateJmsDestinationParameters implements DestinationTypeFunctionParameters {

    private JmsDestination jmsDestination;

    public ValidateJmsDestinationParameters(JmsDestination jmsDestination) {
        this.jmsDestination = jmsDestination;
    }

    public JmsDestination getJmsDestination() {
        return jmsDestination;
    }
}
