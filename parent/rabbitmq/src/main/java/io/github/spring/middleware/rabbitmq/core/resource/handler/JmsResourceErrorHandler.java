package io.github.spring.middleware.rabbitmq.core.resource.handler;

public interface JmsResourceErrorHandler<T> {

    void handleError(ErrorHandlerContext<T> errorHandlerContext);

}
