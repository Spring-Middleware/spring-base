package io.github.spring.middleware.ai.exception;

import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.ErrorDescriptor;

public class AIException extends RuntimeException implements ErrorDescriptor {

    private ErrorCodes errorCodes;

    public AIException(ErrorCodes errorCodes, String message) {
        super(message);
        this.errorCodes = errorCodes;
    }

    public AIException(ErrorCodes errorCodes, String message, Throwable cause) {
        super(message, cause);
        this.errorCodes = errorCodes;
    }

    @Override
    public ErrorCodes getErrorCode() {
        return errorCodes;
    }

}
