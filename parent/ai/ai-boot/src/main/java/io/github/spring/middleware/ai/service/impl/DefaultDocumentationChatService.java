package io.github.spring.middleware.ai.service.impl;

import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.conversation.DefaultConversation;
import io.github.spring.middleware.ai.conversation.client.ConversationClient;
import io.github.spring.middleware.ai.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.response.ChatResponse;
import io.github.spring.middleware.ai.service.ConversationStore;
import io.github.spring.middleware.ai.service.DocumentationChatService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DefaultDocumentationChatService implements DocumentationChatService {

    private final ConversationClient conversationClient;
    private final ConversationStore conversationStore;

    public DefaultDocumentationChatService(ConversationClient conversationClient, ConversationStore conversationStore) {
        this.conversationClient = conversationClient;
        this.conversationStore = conversationStore;
    }


    private String buildSystemMessage() {
        return """
                You are the Spring Middleware documentation assistant.
                
                Answer only using the Spring Middleware documentation context.
                If the answer is not documented yet, say that it is not documented yet.
                
                Be concise, technical, and do not invent APIs.
                """;
    }

    @Override
    public DocumentationConversationResponse startConversation(String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage();

        Conversation conversation = new DefaultConversation();
        conversation.addSystemMessage(systemMessage);

        ChatResponse chatResponse = conversationClient.chat(conversation, model, question);

        UUID conversationId = conversationStore.create(conversation);

        return new DocumentationConversationResponse(conversationId, chatResponse);
    }

    @Override
    public ChatResponse ask(UUID conversationId, String model, String question) {
        validateInput(model, question);
        Conversation conversation = conversationStore.get(conversationId);
        return conversationClient.chat(conversation, model, question);
    }

    private void validateInput(String model, String question) {
        if (model == null || model.isBlank()) {
            throw new AIException(AIErrorCodes.INVALID_AI_MODEL, "AI model must not be null or blank");
        }
        if (question == null || question.isBlank()) {
            throw new AIException(AIErrorCodes.INVALID_AI_REQUEST, "Question must not be null or blank");
        }
    }

}
