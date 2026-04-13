package io.github.spring.middleware.error;

public enum FrameworkErrorCodes implements ErrorCodes {

    UNKNOWN_ERROR("FRAMEWORK:UNKNOWN_ERROR", "An unknown error occurred"),
    PROXY_CLIENT_ERROR("FRAMEWORK:PROXY_CLIENT_ERROR", "An error occurred while calling an external service"),
    PROXY_CLIENT_UNAVAILABLE_ERROR("FRAMEWORK:PROXY_CLIENT_UNAVAILABLE_ERROR", "The external service is currently unavailable"),
    REMOTE_SERVICE_ERROR("FRAMEWORK:REMOTE_SERVICE_ERROR", "An error occurred in a remote service"),
    VALIDATION_ERROR("FRAMEWORK:VALIDATION_ERROR", "A validation error occurred"),
    NOT_FOUND("FRAMEWORK:NOT_FOUND", "The requested resource was not found"),
    ALREADY_EXISTS("FRAMEWORK:ALREADY_EXISTS", "The resource already exists"),
    BAD_REQUEST("FRAMEWORK:BAD_REQUEST", "The request was invalid"),
    DATABASE_CONSTRAINT_ERROR("FRAMEWORK:DATABASE_CONSTRAINT", "An unknown database constraint was violated"),
    CALL_NOT_PERMITTED("FRAMEWORK:CALL_NOT_PERMITTED", "The service is currently unavailable");

    private String code;
    private String message;


    FrameworkErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getCode() {
        return code;
    }
}
