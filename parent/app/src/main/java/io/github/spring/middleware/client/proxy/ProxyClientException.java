package io.github.spring.middleware.client.proxy;

import java.io.Serial;

public class ProxyClientException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected int httpStatusCode;

    public ProxyClientException(Exception e) {
        super(e);
    }

    public ProxyClientException(String message, Exception e) {
        super(message, e);
    }

    public ProxyClientException(String message) {
        super(message);
    }
}
