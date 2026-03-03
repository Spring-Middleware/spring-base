package io.github.spring.middleware.error;

public enum FrameworkErrorCodes implements ErrorDescriptor {

    UNKNOWN_ERROR("UNKNOWN_ERROR", "An unknown error occurred"),
    UNKNOWN_DATABASE_CONSTRAINT_ERROR("UNKNOWN_DATABASE_CONSTRAINT", "An unknown database constraint was violated"),
    UNKNOWN_VALIDATION_ERROR("UNKNOWN_VALIDATION_ERROR", "An unknown validation error occurred"),
    NOT_NULL_CONSTRAINT_ERROR("NOT_NULL", "A required value was null"),
    SIZE_CONSTRAINT_ERROR("SIZE_CONSTRAINT", "A value did not meet size constraints");

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
