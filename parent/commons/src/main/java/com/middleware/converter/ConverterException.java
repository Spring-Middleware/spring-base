package com.middleware.converter;

public class ConverterException extends RuntimeException {

    public ConverterException(Exception cause) {

        super(cause);
    }

    public ConverterException(String message) {

        super(message);
    }
}
