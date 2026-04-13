package io.github.spring.middleware.error;

public enum SecurityErrorCodes implements ErrorCodes {

    MISSING_CREDENTIALS("MISSING_CREDENTIALS", "Authentication credentials are missing or invalid."),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Authentication credentials are invalid."),
    AUTHENTICATION_FAILED("AUTHENTICATION_FAILED", "Authentication failed due to invalid credentials or other issues."),
    OAUTH2_TOKEN_ACQUISITION_ERROR("OAUTH2_TOKEN_ACQUISITION_ERROR", "Failed to acquire OAuth2 token from the authentication provider.");

    private String code;
    private String message;

    SecurityErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}

