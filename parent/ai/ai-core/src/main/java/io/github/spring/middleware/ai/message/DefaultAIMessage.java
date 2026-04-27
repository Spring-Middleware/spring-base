package io.github.spring.middleware.ai.message;

public record DefaultAIMessage(AIRole role, String content) implements AIMessage {

    public DefaultAIMessage {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }

    @Override
    public String toString() {
        return STR."AIMessage{role=\{role}, content='\{content}'}";
    }

    public static DefaultAIMessage system(String content) {
        return new DefaultAIMessage(AIRole.SYSTEM, content);
    }

    public static DefaultAIMessage user(String content) {
        return new DefaultAIMessage(AIRole.USER, content);
    }

    public static DefaultAIMessage assistant(String content) {
        return new DefaultAIMessage(AIRole.ASSISTANT, content);
    }

}
