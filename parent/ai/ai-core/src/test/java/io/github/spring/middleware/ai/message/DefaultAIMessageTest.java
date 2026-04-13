package io.github.spring.middleware.ai.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAIMessageTest {

    @Test
    void testConstructorWithValidArguments() {
        DefaultAIMessage message = new DefaultAIMessage(AIRole.USER, "hello");
        assertEquals(AIRole.USER, message.role());
        assertEquals("hello", message.content());
    }

    @Test
    void testConstructorThrowsExceptionWhenRoleIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new DefaultAIMessage(null, "content");
        });
        assertEquals("role must not be null", exception.getMessage());
    }

    @Test
    void testConstructorThrowsExceptionWhenContentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new DefaultAIMessage(AIRole.USER, null);
        });
        assertEquals("content must not be null", exception.getMessage());
    }

    @Test
    void testSystemFactoryMethod() {
        DefaultAIMessage message = DefaultAIMessage.system("system prompt");
        assertEquals(AIRole.SYSTEM, message.role());
        assertEquals("system prompt", message.content());
    }

    @Test
    void testUserFactoryMethod() {
        DefaultAIMessage message = DefaultAIMessage.user("user prompt");
        assertEquals(AIRole.USER, message.role());
        assertEquals("user prompt", message.content());
    }

    @Test
    void testAssistantFactoryMethod() {
        DefaultAIMessage message = DefaultAIMessage.assistant("assistant reply");
        assertEquals(AIRole.ASSISTANT, message.role());
        assertEquals("assistant reply", message.content());
    }

    @Test
    void testToString() {
        DefaultAIMessage message = new DefaultAIMessage(AIRole.USER, "hello");
        assertEquals("AIMessage{role=USER, content='hello'}", message.toString());
    }
}

