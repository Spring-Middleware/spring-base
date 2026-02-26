package io.github.spring.middleware.rabbitmq.converter;

public class ConverterException extends Exception {

    public ConverterException(Exception cause) {
        super(cause);
    }

    public ConverterException(String message) {
        super(message);
    }
}
