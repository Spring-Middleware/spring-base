package io.github.spring.middleware.rabbitmq.core.resource.consumer.creator;

import io.github.spring.middleware.rabbitmq.core.destination.type.params.QueueDestinationCreationParameters;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;


public class QueueDestinationCreator implements MessageDestinationCreator<QueueDestinationCreationParameters> {

    @Override
    public MessageConsumer createMessageConsumer(QueueDestinationCreationParameters queueConsumerCreationParameters, String messageSelector) throws JMSException {
        // Asegúrate de que si no hay selector, llamas al método simple
        if (messageSelector == null || messageSelector.trim().isEmpty()) {
            return queueConsumerCreationParameters.getSession().createConsumer(queueConsumerCreationParameters.getDestination());
        } else {
            return queueConsumerCreationParameters.getSession().createConsumer(queueConsumerCreationParameters.getDestination(), messageSelector);
        }
    }
}
