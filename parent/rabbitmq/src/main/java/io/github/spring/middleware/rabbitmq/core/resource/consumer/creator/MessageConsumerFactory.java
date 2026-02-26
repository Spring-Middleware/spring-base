package io.github.spring.middleware.rabbitmq.core.resource.consumer.creator;

import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionExecutor;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionType;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeMessageDestinationCreatorFunctionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.VoidParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.params.MessageDestinationCreationParameters;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;


public class MessageConsumerFactory {

    private DestinationTypeFunctionExecutor destinationTypeFunctionExecutor;

    private MessageConsumerFactory(DestinationTypeFunctionExecutor destinationTypeFunctionExecutor) {
        this.destinationTypeFunctionExecutor = destinationTypeFunctionExecutor;
    }

    public static MessageConsumerFactory getInstance(DestinationTypeFunctionExecutor destinationTypeFunctionExecutor) {
        return new MessageConsumerFactory(destinationTypeFunctionExecutor);
    }

    public MessageConsumer createMesssageConsumer(Session session, JmsResourceDestination jmsResourceDestination, String messageSelector) throws JMSException {
        DestinationTypeMessageDestinationCreatorFunctionParameters durabilityMessageConsumerCreatorFunctionParameters = new DestinationTypeMessageDestinationCreatorFunctionParameters(session, jmsResourceDestination);
        MessageDestinationCreationParameters messageConsumerCreationParameters = (MessageDestinationCreationParameters) destinationTypeFunctionExecutor.execute(DestinationTypeFunctionType.CREATE_MESSAGE_CONSUMER_PARAMETERS, jmsResourceDestination.getDestinationType(), durabilityMessageConsumerCreatorFunctionParameters);
        VoidParameters voidParameters = new VoidParameters();
        MessageDestinationCreator messageConsumerCreator = (MessageDestinationCreator) destinationTypeFunctionExecutor.execute(DestinationTypeFunctionType.CREATE_MESSAGE_CONSUMER, jmsResourceDestination.getDestinationType(), voidParameters);
        return messageConsumerCreator.createMessageConsumer(messageConsumerCreationParameters, messageSelector);
    }

}



