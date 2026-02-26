package io.github.spring.middleware.rabbitmq.core.destination.type;

import io.github.spring.middleware.rabbitmq.core.destination.type.params.MessageDestinationCreationParameters;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.creator.MessageDestinationCreator;

public enum DestinationTypeFunctionType {

    VALIDATE_JMS_DESTINATION(Boolean.class),
    CREATE_MESSAGE_CONSUMER_PARAMETERS(MessageDestinationCreationParameters.class),
    CREATE_MESSAGE_CONSUMER(MessageDestinationCreator.class);

    public Class clazzReturned;

    DestinationTypeFunctionType(Class clazzReturned) {
        this.clazzReturned = clazzReturned;
    }

}
