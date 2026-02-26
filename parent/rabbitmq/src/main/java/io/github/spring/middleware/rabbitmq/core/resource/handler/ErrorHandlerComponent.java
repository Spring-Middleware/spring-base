package io.github.spring.middleware.rabbitmq.core.resource.handler;

import io.github.spring.middleware.rabbitmq.core.resource.JmsResourceType;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToIntFunction;

public class ErrorHandlerComponent<T> {

    public void handleError(Class clazz, Collection<JmsResourceErrorHandler<T>> jmsResourceErrorHandlers,
                            ToIntFunction<? super JmsResourceErrorHandler> priorityFunction, Throwable exception, T t,
                            Properties properties, JmsResourceType jmsResourceType) {

        ErrorHandlerContext errorHandlerContext = new ErrorHandlerContext();
        errorHandlerContext.setT(t);
        errorHandlerContext.setProperties(properties);
        errorHandlerContext.setJmsResourceType(jmsResourceType);
        errorHandlerContext.setException(exception);
        errorHandlerContext.setClazz(clazz);
        CompletableFuture.runAsync(() -> {
            CollectionUtils.emptyIfNull(jmsResourceErrorHandlers).stream()
                    .sorted(Comparator.comparingInt(priorityFunction::applyAsInt))
                    .forEach(jmsErrorHandlerResource -> {
                        jmsErrorHandlerResource.handleError(errorHandlerContext);
                    });
        });
    }

}
