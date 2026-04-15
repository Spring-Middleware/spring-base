package io.github.spring.middleware.error;

import java.util.Map;

public class DefaultErrorDescriptor implements ErrorDescriptor {

    private ErrorCodes errorCodes;

    public DefaultErrorDescriptor(ErrorCodes errorCodes) {
        this.errorCodes = errorCodes;
    }

    public String getMessage() {
        return errorCodes.getMessage();
    }

    @Override
    public ErrorCodes getErrorCode() {
        return errorCodes;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return Map.of();
    }
}
