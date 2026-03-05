package io.github.spring.middleware.exception;

import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ServiceException extends RuntimeException implements ErrorDescriptor {

    private final HttpStatus httpStatus;
    private final ErrorDescriptor descriptor;
    private final Map<String, Object> extensions;

    protected ServiceException(HttpStatus httpStatus, ErrorDescriptor descriptor, String message) {
        this(httpStatus, descriptor, message, null, null);
    }

    protected ServiceException(HttpStatus httpStatus, ErrorDescriptor descriptor, String message, Throwable cause) {
        this(httpStatus, descriptor, message, cause, null);
    }

    protected ServiceException(HttpStatus httpStatus,
                               ErrorDescriptor descriptor,
                               String message,
                               Throwable cause,
                               Map<String, Object> extensions) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.descriptor = descriptor;
        Map<String, Object> merged = new HashMap<>();
        if (descriptor != null && descriptor.getExtensions() != null) merged.putAll(descriptor.getExtensions());
        if (extensions != null) merged.putAll(extensions);
        this.extensions = Collections.unmodifiableMap(merged);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return descriptor != null ? descriptor.getCode() : FrameworkErrorCodes.UNKNOWN_ERROR.getCode();
    }

    @Override
    public String getMessage() {
        // El message de RuntimeException manda (más específico del contexto)
        return super.getMessage();
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }
}