package io.github.spring.middleware.kafka.core.error;

public interface KafkaExceptionClassifier {

    Boolean classify(Throwable exception);

}
