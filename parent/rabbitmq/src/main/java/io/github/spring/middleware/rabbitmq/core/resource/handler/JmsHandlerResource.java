package io.github.spring.middleware.rabbitmq.core.resource.handler;


import jakarta.jms.JMSException;

import java.util.Properties;
import java.util.function.Consumer;

public class JmsHandlerResource<R, T> {

    public <H extends HandlerParameters> void handle(Consumer<H> processorMessage, H handlerParameters) {
        R r = null;
        try {
            r = handleBeforeProcessingMessage((T) handlerParameters.getMessage(), handlerParameters.getProperties());
            processorMessage.accept(handlerParameters);
            handleAfterProcessingMessage(r, (T) handlerParameters.getMessage(), handlerParameters.getProperties());
        } catch (RuntimeException ex) {
            handleExceptionProcessinggMessage(ex, r, (T) handlerParameters.getMessage(), handlerParameters.getProperties());
        } catch (Exception ex) {
            handleExceptionProcessinggMessage(ex, r, (T) handlerParameters.getMessage(), handlerParameters.getProperties());
        } finally {
            handleFinallyProcessingMessage(r, (T) handlerParameters.getMessage(), handlerParameters.getProperties());
        }
    }

    public R handleBeforeProcessingMessage(T t, Properties properties) throws JMSException {
        return null;
    }

    public void handleExceptionProcessinggMessage(Exception ex, R r, T t, Properties properties) {
    }

    public void handleAfterProcessingMessage(R r, T t, Properties properties) throws JMSException {
    }

    public void handleFinallyProcessingMessage(R r, T t, Properties properties) {
    }


}
