package io.github.spring.middleware.kafka.core.error;

import java.util.Set;

public interface KafkaExceptionProvider {
    Class<? extends Exception>[] getNoRetryableExceptions();
}