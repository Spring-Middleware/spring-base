package io.github.spring.middleware.kafka.core.exception;

import io.github.spring.middleware.error.ErrorDescriptor;

import java.util.Map;

public class KafkaException extends RuntimeException implements ErrorDescriptor {

    private ErrorDescriptor errorDescriptor;

    public KafkaException(ErrorDescriptor errorDescriptor, String message) {
        super(message);
        this.errorDescriptor = errorDescriptor;
    }

    public KafkaException(ErrorDescriptor errorDescriptor, String message, Throwable cause) {
        super(message, cause);
        this.errorDescriptor = errorDescriptor;
    }

    @Override
    public String getCode() {
        return errorDescriptor.getCode();
    }

    @Override
    public Map<String, Object> getExtensions() {
        return errorDescriptor.getExtensions();
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
