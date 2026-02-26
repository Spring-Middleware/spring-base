package io.github.spring.middleware.rabbitmq.resources.handler;


import io.github.spring.middleware.rabbitmq.annotations.JmsHandler;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsHandlerResource;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@JmsHandler(value = ProducerHandler.class)
public class ProducerHandlerResource<T> extends JmsHandlerResource<String, T> {

    private Logger logger = LoggerFactory.getLogger(ConsumerHandlerResource.class);

    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) throws JMSException {
        String handlingMessage = "Iniciando session de DB";
        logger.debug("handleBeforeSendingMessage " + handlingMessage);
        properties.setProperty("ENVIRONMENT","STABLE");
        return handlingMessage;
    }

    @Override
    public void handleFinallyProcessingMessage(String handlingMessage, T t, Properties properties) {
        logger.debug("handleFinallySendingMessage " + handlingMessage);
    }

}
