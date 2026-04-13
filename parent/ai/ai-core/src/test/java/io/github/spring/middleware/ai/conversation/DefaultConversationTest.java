package io.github.spring.middleware.ai.conversation;

import io.github.spring.middleware.ai.message.AIMessage;
import io.github.spring.middleware.ai.message.AIRole;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.request.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultConversationTest {

    private DefaultConversation conversation;

    @BeforeEach
    void setUp() {
        conversation = new DefaultConversation();
    }

    @Test
    void testInitialState() {
        assertTrue(conversation.isEmpty());
        assertEquals(0, conversation.size());
        assertTrue(conversation.getMessages().isEmpty());
    }

    @Test
    void testAddMessage() {
        AIMessage message = DefaultAIMessage.user("Hello");
        conversation.addMessage(message);

        assertFalse(conversation.isEmpty());
        assertEquals(1, conversation.size());

        List<AIMessage> messages = conversation.getMessages();
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
    }

    @Test
    void testAddMessageThrowsExceptionOnNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversation.addMessage(null);
        });
        assertEquals("message must not be null", exception.getMessage());
    }

    @Test
    void testAddSystemMessage() {
        conversation.addSystemMessage("System instructions");
        assertEquals(1, conversation.size());
        AIMessage message = conversation.getMessages().get(0);
        assertEquals(AIRole.SYSTEM, message.role());
        assertEquals("System instructions", message.content());
    }

    @Test
    void testAddUserMessage() {
        conversation.addUserMessage("User question");
        assertEquals(1, conversation.size());
        AIMessage message = conversation.getMessages().get(0);
        assertEquals(AIRole.USER, message.role());
        assertEquals("User question", message.content());
    }

    @Test
    void testAddAssistantMessage() {
        conversation.addAssistantMessage("Assistant reply");
        assertEquals(1, conversation.size());
        AIMessage message = conversation.getMessages().get(0);
        assertEquals(AIRole.ASSISTANT, message.role());
        assertEquals("Assistant reply", message.content());
    }

    @Test
    void testToRequest() {
        conversation.addUserMessage("Hello");
        conversation.addAssistantMessage("Hi there");

        ChatRequest request = conversation.toRequest("test-model");

        assertNotNull(request);
        assertEquals("test-model", request.getModel());

        List<AIMessage> requestMessages = request.getMessages();
        assertEquals(2, requestMessages.size());
        assertEquals(AIRole.USER, requestMessages.get(0).role());
        assertEquals("Hello", requestMessages.get(0).content());
        assertEquals(AIRole.ASSISTANT, requestMessages.get(1).role());
        assertEquals("Hi there", requestMessages.get(1).content());
    }

    @Test
    void testGetMessagesReturnsImmutableList() {
        conversation.addUserMessage("msg");
        List<AIMessage> list = conversation.getMessages();
        assertThrows(UnsupportedOperationException.class, () -> list.add(DefaultAIMessage.user("another")));
    }
}

