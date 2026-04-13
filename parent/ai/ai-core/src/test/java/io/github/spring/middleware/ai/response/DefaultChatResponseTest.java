package io.github.spring.middleware.ai.response;

import io.github.spring.middleware.ai.message.AIMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class DefaultChatResponseTest {

    @Test
    void testConstructorWithValidArgument() {
        AIMessage mockMessage = Mockito.mock(AIMessage.class);
        DefaultChatResponse response = new DefaultChatResponse(mockMessage);

        assertEquals(mockMessage, response.getMessage());
    }

    @Test
    void testConstructorThrowsExceptionWhenMessageIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new DefaultChatResponse(null);
        });

        assertEquals("message must not be null", exception.getMessage());
    }

    @Test
    void testToString() {
        AIMessage mockMessage = Mockito.mock(AIMessage.class);
        Mockito.when(mockMessage.toString()).thenReturn("MockedMessage");

        DefaultChatResponse response = new DefaultChatResponse(mockMessage);
        String result = response.toString();

        assertTrue(result.contains("DefaultChatResponse"));
        assertTrue(result.contains("MockedMessage"));
    }
}

