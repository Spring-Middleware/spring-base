package io.github.spring.middleware.ai.conversation.client;

import io.github.spring.middleware.ai.client.ChatClient;
import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.message.AIMessage;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DefaultConversationClientTest {

    private ChatClient chatClient;
    private DefaultConversationClient conversationClient;

    @BeforeEach
    void setUp() {
        chatClient = Mockito.mock(ChatClient.class);
        conversationClient = new DefaultConversationClient(chatClient);
    }

    @Test
    void testChatThrowsExceptionWhenConversationIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(null, "model", "hello");
        });
        assertEquals("conversation must not be null", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenModelIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, null, "hello");
        });
        assertEquals("model must not be null or blank", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenModelIsBlank() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, "  ", "hello");
        });
        assertEquals("model must not be null or blank", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenUserMessageIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, "model", null);
        });
        assertEquals("userMessage must not be null", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenResponseIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        when(conversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            conversationClient.chat(conversation, "model", "hello");
        });
        assertEquals("chat response must not be null", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenResponseMessageIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);

        when(conversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(chatResponse);
        when(chatResponse.getMessage()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            conversationClient.chat(conversation, "model", "hello");
        });
        assertEquals("chat response must not be null", exception.getMessage());
    }

    @Test
    void testChatSuccessfully() {
        Conversation conversation = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        AIMessage aiMessage = Mockito.mock(AIMessage.class);

        when(conversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(chatResponse);
        when(chatResponse.getMessage()).thenReturn(aiMessage);

        ChatResponse response = conversationClient.chat(conversation, "model", "hello user");

        assertEquals(chatResponse, response);

        verify(conversation).addUserMessage("hello user");
        verify(conversation).addMessage(aiMessage);
        verify(chatClient).generate(chatRequest);
    }
}

