package io.github.spring.middleware.rabbitmq.core.resource.consumer.creator;

import io.github.spring.middleware.rabbitmq.core.destination.type.params.DurableSubscriberCreationParameters;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;


public class DurableSubscriberCreator implements MessageDestinationCreator<DurableSubscriberCreationParameters> {

    @Override
    public MessageConsumer createMessageConsumer(DurableSubscriberCreationParameters durableSubscriberCreationParameters, String messageSelector) throws JMSException {
        return durableSubscriberCreationParameters.getSession().createDurableSubscriber(durableSubscriberCreationParameters.getDestination(), durableSubscriberCreationParameters.getTopicId());
    }
}
