package io.github.spring.middleware.registry.exceptions;

import java.io.Serial;

public class PathInvalidException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PathInvalidException(String message) {
        super(message);
    }

    public PathInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
