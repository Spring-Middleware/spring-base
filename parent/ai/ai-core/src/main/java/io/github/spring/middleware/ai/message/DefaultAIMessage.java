package io.github.spring.middleware.ai.message;

public class DefaultAIMessage implements AIMessage {

    private final AIRole role;
    private final String content;

    public DefaultAIMessage(AIRole role, String content) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        this.role = role;
        this.content = content;
    }

    @Override
    public AIRole role() {
        return this.role;
    }

    @Override
    public String content() {
        return this.content;
    }

    @Override
    public String toString() {
        return "AIMessage{" +
                "role=" + role +
                ", content='" + content + '\'' +
                '}';
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
