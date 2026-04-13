package io.github.spring.middleware.ai.response;

import io.github.spring.middleware.ai.message.AIMessage;

public class DefaultChatResponse implements ChatResponse {

    private final AIMessage message;

    public DefaultChatResponse(AIMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        this.message = message;
    }

    @Override
    public AIMessage getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "DefaultChatResponse{" +
                "message=" + message +
                '}';
    }
}
