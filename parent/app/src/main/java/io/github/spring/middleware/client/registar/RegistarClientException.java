package io.github.spring.middleware.client.registar;

import java.io.Serial;

public class RegistarClientException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RegistarClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistarClientException(String message) {
        super(message);
    }
}
