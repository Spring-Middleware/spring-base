package io.github.spring.middleware.kafka.core.exception;

import io.github.spring.middleware.error.ErrorCodes;

public enum KafkaErrorCodes implements ErrorCodes {

    KAFKA_TOPIC_NOT_CONFIGURED("KAFKA_TOPIC_NOT_CONFIGURED"),
    KAFKA_PUBLISHER_NOT_FOUND("KAFKA_PUBLISHER_NOT_FOUND"),
    KAFKA_PUBLISHER_ALREADY_REGISTERED("KAFKA_PUBLISHER_ALREADY_REGISTERED"),
    KAFKA_EVENT_NULL("KAFKA_EVENT_NULL"),
    KAFKA_SUBSCRIBER_NOT_FOUND("KAFKA_SUBSCRIBER_NOT_FOUND"),
    KAFKA_PUBLISH_RETRIES_EXHAUSTED("KAFKA_PUBLISH_RETRIES_EXHAUSTED");

    private final String code;

    KafkaErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}
