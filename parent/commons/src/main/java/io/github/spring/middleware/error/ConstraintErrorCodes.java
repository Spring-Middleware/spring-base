package io.github.spring.middleware.error;

public enum ConstraintErrorCodes implements ErrorDescriptor {

    NOT_NULL_CONSTRAINT_ERROR("NOT_NULL", "A required value was null"),
    SIZE_CONSTRAINT_ERROR("SIZE_CONSTRAINT", "A value did not meet size constraints");

    private String code;
    private String message;


    ConstraintErrorCodes(String code, String message) {
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




