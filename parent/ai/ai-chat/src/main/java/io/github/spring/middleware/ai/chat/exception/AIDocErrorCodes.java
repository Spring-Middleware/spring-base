package io.github.spring.middleware.ai.chat.exception;

import io.github.spring.middleware.error.ErrorCodes;

public enum AIDocErrorCodes implements ErrorCodes {

    CONVERSATION_NOT_FOUND("CONVERSATION_NOT_FOUND");

    private final String code;

    AIDocErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
