package io.github.spring.middleware.ai.chat.exception;

import io.github.spring.middleware.ai.exception.AIException;

public class ConversationNotFondException extends AIException {

    public ConversationNotFondException(String message) {
        super(AIDocErrorCodes.CONVERSATION_NOT_FOUND, message);
    }

    public ConversationNotFondException(String message, Throwable cause) {
        super(AIDocErrorCodes.CONVERSATION_NOT_FOUND, message, cause);
    }
}
