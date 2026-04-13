package io.github.spring.middleware.ai.request;

import io.github.spring.middleware.ai.message.AIMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultChatRequestTest {

    @Test
    void testConstructorAndGetModel() {
        DefaultChatRequest request = new DefaultChatRequest("test-model");
        assertEquals("test-model", request.getModel());
    }

    @Test
    void testAddMessageAndGetMessages() {
        DefaultChatRequest request = new DefaultChatRequest("test-model");

        AIMessage message1 = Mockito.mock(AIMessage.class);
        AIMessage message2 = Mockito.mock(AIMessage.class);

        request.addMessage(message1);
        request.addMessage(message2);

        List<AIMessage> messages = request.getMessages();
        assertEquals(2, messages.size());
        assertEquals(message1, messages.get(0));
        assertEquals(message2, messages.get(1));
    }

    @Test
    void testGetMessagesReturnsImmutableList() {
        DefaultChatRequest request = new DefaultChatRequest("test-model");
        List<AIMessage> messages = request.getMessages();

        assertThrows(UnsupportedOperationException.class, () -> {
            messages.add(Mockito.mock(AIMessage.class));
        });
    }

    @Test
    void testToString() {
        DefaultChatRequest request = new DefaultChatRequest("my-model");
        String result = request.toString();
        assertTrue(result.contains("my-model"));
        assertTrue(result.contains("DefaultChatRequest"));
    }
}

