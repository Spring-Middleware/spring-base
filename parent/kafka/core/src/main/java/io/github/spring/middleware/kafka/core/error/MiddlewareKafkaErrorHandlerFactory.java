package io.github.spring.middleware.kafka.core.error;

import org.springframework.kafka.listener.CommonErrorHandler;

public interface MiddlewareKafkaErrorHandlerFactory<E extends CommonErrorHandler> {

    CommonErrorHandler buildErrorHandler();

}
