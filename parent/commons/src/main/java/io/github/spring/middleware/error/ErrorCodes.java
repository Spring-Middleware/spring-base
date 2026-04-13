package io.github.spring.middleware.error;

public interface ErrorCodes {

    String getCode();

    default String getMessage() {
        return "An error occurred";
    }
}
