package io.github.spring.middleware.kafka.core.error;

import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Component
@RequiredArgsConstructor
public class DefaultMiddlewareKafkaErrorHandlerFactory implements MiddlewareKafkaErrorHandlerFactory<DefaultErrorHandler> {

    private final KafkaProperties kafkaProperties;
    private final KafkaExceptionProvider kafkaExceptionProvider;
    private final DeadLetterPublishingRecoverer deadLetterPublishingRecoverer;

    @Override
    public DefaultErrorHandler buildErrorHandler() {
        final var errorHandling = kafkaProperties.getErrorHandling();
        if (!errorHandling.isEnabled()) {
            return null;
        }
        FixedBackOff backOff = new FixedBackOff(errorHandling.getRetryBackoffMs(), errorHandling.getMaxRetries());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);
        errorHandler.addNotRetryableExceptions(kafkaExceptionProvider.getNoRetryableExceptions());
        return errorHandler;
    }

}
