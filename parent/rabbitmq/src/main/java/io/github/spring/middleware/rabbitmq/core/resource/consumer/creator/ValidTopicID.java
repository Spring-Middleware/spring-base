package io.github.spring.middleware.rabbitmq.core.resource.consumer.creator;

import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionResult;

public class ValidTopicID implements DestinationTypeFunctionResult {

    private boolean valid;

    public ValidTopicID(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }
}
