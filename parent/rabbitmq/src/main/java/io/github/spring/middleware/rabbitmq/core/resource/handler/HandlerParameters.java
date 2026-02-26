package io.github.spring.middleware.rabbitmq.core.resource.handler;

import java.util.Properties;

public class HandlerParameters<T> {

    private T message;
    private Properties properties;
    protected boolean handlerError;

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean isHandlerError() {

        return handlerError;
    }

    public void setHandlerError(boolean handlerError) {

        this.handlerError = handlerError;
    }
}
