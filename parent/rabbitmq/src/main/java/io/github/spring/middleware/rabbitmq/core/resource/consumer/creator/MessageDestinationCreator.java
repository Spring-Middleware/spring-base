package io.github.spring.middleware.rabbitmq.core.resource.consumer.creator;

import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionResult;
import io.github.spring.middleware.rabbitmq.core.destination.type.params.MessageDestinationCreationParameters;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;


public interface MessageDestinationCreator<P extends MessageDestinationCreationParameters> extends DestinationTypeFunctionResult {

    MessageConsumer createMessageConsumer(P consumerCreationParameters, String messageSelector) throws JMSException;

}
