package io.github.spring.middleware.error;

public enum FrameworkErrorCodes implements ErrorDescriptor {

    UNKNOWN_ERROR("FRAMEWORK:UNKNOWN_ERROR", "An unknown error occurred"),
    VALIDATION_ERROR("FRAMEWORK:VALIDATION_ERROR", "A validation error occurred"),
    NOT_FOUND("FRAMEWORK:NOT_FOUND", "The requested resource was not found"),
    ALREADY_EXISTS("FRAMEWORK:ALREADY_EXISTS", "The resource already exists"),
    BAD_REQUEST("FRAMEWORK:BAD_REQUEST", "The request was invalid"),
    DATABASE_CONSTRAINT_ERROR("FRAMEWORK:DATABASE_CONSTRAINT", "An unknown database constraint was violated");

    private String code;
    private String message;


    FrameworkErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getCode() {
        return code;
    }
}
