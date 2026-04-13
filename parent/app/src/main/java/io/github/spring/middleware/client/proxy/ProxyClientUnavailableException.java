package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorCodes;

import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProxyClientUnavailableException extends RuntimeException implements ErrorDescriptor {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> extensions;

    public ProxyClientUnavailableException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public ProxyClientUnavailableException(String message, Throwable cause, Map<String, Object> extensions) {
        super(message, cause);
        this.extensions = extensions != null ? new HashMap<>(extensions) : new HashMap<>();
    }

    public ProxyClientUnavailableException(String message) {
        super(message);
        this.extensions = new HashMap<>();
    }

    @Override
    public ErrorCodes getCode() {
        return FrameworkErrorCodes.PROXY_CLIENT_UNAVAILABLE_ERROR;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    public void addExtension(String key, Object value) {
        if (key != null) {
            extensions.put(key, value);
        }
    }
}
