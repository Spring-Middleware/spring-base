package io.github.spring.middleware.rabbitmq.core.resource.consumer;

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ExceptionConnectionListener implements ExceptionListener {

    private Logger logger = LoggerFactory.getLogger(ExceptionConnectionListener.class);
    private JmsConsumerResource consumerResource;

    public ExceptionConnectionListener(JmsConsumerResource consumerResource) {

        this.consumerResource = consumerResource;
    }

    @Override
    public void onException(JMSException ex) {

        logger.error("Error on connection for resource " + consumerResource.getClass().getName(), ex);
        try {
            CompletableFuture.supplyAsync(() -> {
                consumerResource.stop(true);
                return null;
            }).get(3000, TimeUnit.MILLISECONDS);

        } catch (Exception iex) {
            logger.error("Can't stop " + consumerResource.getClass().getSimpleName(), iex);
        }
        consumerResource.start(true);
    }
}
