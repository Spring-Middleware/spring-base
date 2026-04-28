package io.github.spring.middleware.ai.service.impl;

import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.conversation.DefaultConversation;
import io.github.spring.middleware.ai.conversation.client.ConversationClient;
import io.github.spring.middleware.ai.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.rag.context.RagContext;
import io.github.spring.middleware.ai.rag.context.RagContextBuilder;
import io.github.spring.middleware.ai.rag.context.RagContextRequest;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import io.github.spring.middleware.ai.response.ChatResponse;
import io.github.spring.middleware.ai.service.ConversationStore;
import io.github.spring.middleware.ai.service.DocumentationChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultDocumentationChatService implements DocumentationChatService {

    private final RagContextBuilder ragContextBuilder;
    private final ConversationClient conversationClient;
    private final ConversationStore conversationStore;
    private final DocumentIndexingProperties documentIndexingProperties;


    private String buildSystemMessage() {
        return """
                You are the Spring Middleware documentation assistant.
                
                Answer only using the Spring Middleware documentation context.
                If the answer is not documented yet, say that it is not documented yet.
                
                Be concise, technical, and do not invent APIs.
                """;
    }

    private String buildContext(String model, String question) {
        RagContext ragContext = ragContextBuilder.build(new RagContextRequest(documentIndexingProperties.getEmbeddingModel(), VectorType.MONGO, question, documentIndexingProperties.getTopK()));
        return ragContext.content();
    }

    @Override
    public DocumentationConversationResponse startConversation(String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage();

        Conversation conversation = new DefaultConversation();
        conversation.addSystemMessage(systemMessage);

        String context = buildContext(model, question);
        ChatResponse chatResponse = conversationClient.chat(conversation, model, question, context);

        UUID conversationId = conversationStore.create(conversation);

        return new DocumentationConversationResponse(conversationId, chatResponse);
    }

    @Override
    public ChatResponse ask(UUID conversationId, String model, String question) {
        validateInput(model, question);
        Conversation conversation = conversationStore.get(conversationId);
        String context = buildContext(model, question);
        return conversationClient.chat(conversation, model, question, context);
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
