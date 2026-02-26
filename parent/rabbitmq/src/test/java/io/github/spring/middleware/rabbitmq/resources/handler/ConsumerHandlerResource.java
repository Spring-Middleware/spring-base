package io.github.spring.middleware.rabbitmq.resources.handler;

import io.github.spring.middleware.rabbitmq.annotations.JmsHandler;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsHandlerResource;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@JmsHandler(ConsumerHandler.class)
public class ConsumerHandlerResource<T> extends JmsHandlerResource<String, T> {

    private Logger logger = LoggerFactory.getLogger(ConsumerHandlerResource.class);

    @Override
    public String handleBeforeProcessingMessage(T message, Properties properties) throws JMSException {
        String handlingMessage = "Iniciando session de DB";
        logger.debug("handleBeforeConsumingMessage " + handlingMessage);
        return handlingMessage;
    }


    public void handleFinallyConsumingMessage(String handlingMessage) {
        logger.debug("handleFinallyConsumingMessage " + handlingMessage);
    }
}
