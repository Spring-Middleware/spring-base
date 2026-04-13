package io.github.spring.middleware.kafka.core.exception;

import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.ErrorDescriptor;

import java.util.HashMap;
import java.util.Map;

public class KafkaException extends RuntimeException implements ErrorDescriptor {

    private KafkaErrorCodes errorCodes;

    public KafkaException(KafkaErrorCodes errorCodes, String message) {
        super(message);
        this.errorCodes = errorCodes;
    }

    public KafkaException(KafkaErrorCodes errorCodes, String message, Throwable cause) {
        super(message, cause);
        this.errorCodes = errorCodes;
    }

    @Override
    public ErrorCodes getCode() {
        return errorCodes;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
