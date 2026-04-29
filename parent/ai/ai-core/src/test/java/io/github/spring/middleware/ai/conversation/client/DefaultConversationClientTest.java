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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultConversationClientTest {

    private ChatClient chatClient;
    private DefaultConversationClient conversationClient;

    private final String ARGUMENTED_TEMPLATE = """
            Documentation context:
            
            %s
            
            User question:
            
            %s
            """;

    @BeforeEach
    void setUp() {
        chatClient = Mockito.mock(ChatClient.class);
        conversationClient = new DefaultConversationClient(chatClient);
    }

    @Test
    void testChatThrowsExceptionWhenConversationIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(null, "model", "hello", "context");
        });
        assertEquals("conversation must not be null", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenModelIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, null, "hello", "context");
        });
        assertEquals("model must not be null or blank", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenModelIsBlank() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, "  ", "hello", "context");
        });
        assertEquals("model must not be null or blank", exception.getMessage());
    }

    @Test
    void testChatThrowsExceptionWhenUserMessageIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            conversationClient.chat(conversation, "model", null, "context");
        });
        assertEquals("userMessage must not be null", exception.getMessage());
    }

    @Test
    void testChatAddsUserMessageToConversationWhenChatResponseIsNull() {
        Conversation conversation = Mockito.mock(Conversation.class);
        Conversation requestConversation = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        when(conversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(null);
        when(conversation.copy()).thenReturn(requestConversation);


        conversationClient.chat(conversation, "model", "hello", "context");
        verify(conversation).addUserMessage("hello");
        verify(requestConversation).addUserMessage(ARGUMENTED_TEMPLATE.formatted("context","hello"));
    }

    @Test
    void testChatAddsUserMessageToConversation() {
        Conversation conversation = Mockito.mock(Conversation.class);
        Conversation requestConverstion = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);

        when(conversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(chatResponse);
        when(chatResponse.getMessage()).thenReturn(null);
        when(conversation.copy()).thenReturn(requestConverstion);


        conversationClient.chat(conversation, "model", "hello", "context");
        verify(conversation, times(1)).addUserMessage("hello");
        verify(requestConverstion, times(1)).addUserMessage(ARGUMENTED_TEMPLATE.formatted("context", "hello"));
    }

    @Test
    void testChatSuccessfully() {
        Conversation conversation = Mockito.mock(Conversation.class);
        Conversation requestConversation = Mockito.mock(Conversation.class);
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        ChatResponse chatResponse = Mockito.mock(ChatResponse.class);
        AIMessage aiMessage = Mockito.mock(AIMessage.class);

        when(requestConversation.toRequest("model")).thenReturn(chatRequest);
        when(chatClient.generate(chatRequest)).thenReturn(chatResponse);
        when(chatResponse.getMessage()).thenReturn(aiMessage);
        when(conversation.copy()).thenReturn(requestConversation);

        ChatResponse response = conversationClient.chat(conversation, "model", "hello user", "context");

        assertEquals(chatResponse, response);

        verify(conversation).addUserMessage("hello user");
        verify(conversation).addMessage(aiMessage);
        verify(requestConversation).addUserMessage(ARGUMENTED_TEMPLATE.formatted("context", "hello user"));
        verify(chatClient).generate(chatRequest);
    }
}

