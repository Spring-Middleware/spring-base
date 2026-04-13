package io.github.spring.middleware.ai.exception;

import io.github.spring.middleware.error.ErrorCodes;

public enum AIErrorCodes implements ErrorCodes {

    AI_PROVIDER_NOT_FOUND("AI_PROVIDER_NOT_FOUND"),
    UNSUPPORTED_AI_MODEL("UNSUPPORTED_AI_MODEL"),
    AI_RESPONSE_MAPPING_ERROR("AI_RESPONSE_MAPPING_ERROR"),
    INVALID_AI_REQUEST("INVALID_AI_REQUEST"),
    INVALID_AI_MODEL("INVALID_AI_MODEL"),
    AI_RESPONSE_ERROR("AI_RESPONSE_ERROR");

    private final String code;

    AIErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}
