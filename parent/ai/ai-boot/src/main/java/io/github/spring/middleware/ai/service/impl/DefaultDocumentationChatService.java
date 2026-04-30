package io.github.spring.middleware.ai.service.impl;

import io.github.spring.middleware.ai.client.ChatClient;
import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.conversation.DefaultConversation;
import io.github.spring.middleware.ai.conversation.client.ConversationClient;
import io.github.spring.middleware.ai.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceDefinition;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceProperties;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.rag.context.RagContext;
import io.github.spring.middleware.ai.rag.context.RagContextBuilder;
import io.github.spring.middleware.ai.rag.context.RagContextRequest;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.request.DefaultChatRequest;
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
    private final ChatClient chatClient;
    private final ConversationStore conversationStore;
    private final DocumentIndexingProperties documentIndexingProperties;
    private final DocumentSourceProperties documentSourceProperties;


    private String buildSystemMessage(String sourceName) {
        DocumentSourceDefinition sourceDefinition = documentSourceProperties.getSources().get(sourceName);
        if (sourceDefinition == null) {
            throw new AIException(AIErrorCodes.INVALID_DOCUMENT_SOURCE, STR."No document source definition found for sourceName: \{sourceName}");
        }
        return sourceDefinition.getSystemContext();
    }

    private String buildContext(String sourceName, String question) {
        DocumentIndexingProperties.DocumentIndexingSourceProperties sourceProperties = documentIndexingProperties.getSources().get(sourceName);
        if (sourceProperties == null) {
            throw new AIException(AIErrorCodes.INVALID_DOCUMENT_SOURCE, STR."No indexing properties found for sourceName: \{sourceName}");
        }
        RagContext ragContext = ragContextBuilder.build(new RagContextRequest(sourceProperties.getEmbeddingModel(), sourceProperties.getVectorType(), new VectorNamespace(sourceProperties.getVectorNamespace(sourceName)), question, sourceProperties.getTopK()));
        return ragContext.content();
    }

    @Override
    public DocumentationConversationResponse startConversation(String sourceName, String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage(sourceName);

        Conversation conversation = new DefaultConversation();
        conversation.addSystemMessage(systemMessage);

        String context = buildContext(sourceName, question);
        ChatResponse chatResponse = conversationClient.chat(conversation, model, question, context);

        UUID conversationId = conversationStore.create(conversation);

        return new DocumentationConversationResponse(conversationId, chatResponse);
    }

    @Override
    public ChatResponse ask(UUID conversationId, String sourceName, String model, String question) {
        validateInput(model, question);
        Conversation conversation = conversationStore.get(conversationId);
        String context = buildContext(sourceName, question);
        return conversationClient.chat(conversation, model, question, context);
    }

    @Override
    public ChatResponse ask(String sourceName, String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage(sourceName);
        String context = buildContext(sourceName, question);
        ChatRequest chatRequest = new DefaultChatRequest(model);
        chatRequest.addMessage(DefaultAIMessage.system(systemMessage));
        chatRequest.addMessage(DefaultAIMessage.system(context));
        chatRequest.addMessage(DefaultAIMessage.user(question));
        return chatClient.generate(chatRequest);
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
