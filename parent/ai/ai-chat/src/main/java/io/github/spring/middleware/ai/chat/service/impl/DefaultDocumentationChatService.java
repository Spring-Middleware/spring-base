package io.github.spring.middleware.ai.chat.service.impl;

import io.github.spring.middleware.ai.chat.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.chat.service.ConversationStore;
import io.github.spring.middleware.ai.chat.service.DocumentationChatService;
import io.github.spring.middleware.ai.client.ChatClient;
import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.conversation.DefaultConversation;
import io.github.spring.middleware.ai.conversation.client.ConversationClient;
import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceDefinition;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceProperties;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.rag.context.RagContext;
import io.github.spring.middleware.ai.rag.context.RagContextBuilder;
import io.github.spring.middleware.ai.rag.context.RagContextRequest;
import io.github.spring.middleware.ai.rag.index.config.DocumentIndexingProperties;
import io.github.spring.middleware.ai.rag.planner.RagQueryPlanner;
import io.github.spring.middleware.ai.rag.planner.SelfQueryRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.request.DefaultChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDocumentationChatService implements DocumentationChatService {

    private final RagContextBuilder ragContextBuilder;
    private final ConversationClient conversationClient;
    private final ChatClient chatClient;
    private final ConversationStore conversationStore;
    private final DocumentIndexingProperties documentIndexingProperties;
    private final DocumentSourceProperties documentSourceProperties;
    private final RagQueryPlanner queryPlanner;

    private String buildSystemMessage(String sourceName) {
        DocumentSourceDefinition sourceDefinition = documentSourceProperties.getSources().get(sourceName);
        if (sourceDefinition == null) {
            throw new AIException(AIErrorCodes.INVALID_DOCUMENT_SOURCE, STR."No document source definition found for sourceName: \{sourceName}");
        }
        return sourceDefinition.getSystemContext();
    }

    private Mono<String> buildContext(String sourceName, String model, String question) {
        DocumentIndexingProperties.DocumentIndexingSourceProperties sourceProperties = documentIndexingProperties.getSources().get(sourceName);
        if (sourceProperties == null) {
            throw new AIException(AIErrorCodes.INVALID_DOCUMENT_SOURCE, STR."No indexing properties found for sourceName: \{sourceName}");
        }

        return queryPlanner.plan(new SelfQueryRequest(sourceName, model, sourceProperties.getPlannerContext(), question, sourceProperties.getChunker()))
                .flatMap(plan -> {
                    if (plan.useSemanticSearch() && !plan.filters().isEmpty()) {
                        log.info(STR."RAG query plan for sourceName \{sourceName} indicates to use semantic search with optimized query: \{plan.optimizedQuery()} and filter \{plan.filters()}");
                    }else if (plan.useSemanticSearch() && plan.filters().isEmpty()) {
                        log.info(STR."RAG query plan for sourceName \{sourceName} indicates to use semantic search with optimized query: \{plan.optimizedQuery()}");
                    } else {
                        log.info(STR."RAG query plan for sourceName \{sourceName} indicates to use metadata filters: \{plan.filters()}");
                    }
                    return ragContextBuilder.build(new RagContextRequest(
                            sourceProperties.getEmbeddingModel(),
                            sourceProperties.getVectorType(),
                            new VectorNamespace(sourceProperties.getVectorNamespace(sourceName)),
                            plan.useSemanticSearch() ? plan.optimizedQuery() : "",
                            sourceProperties.getTopK(),
                            plan.filters()
                    ));
                }).map(RagContext::content)
                .onErrorResume(error -> {
                    log.warn(STR."Failed to build RAG context using query planner for sourceName \{}. Falling back to default context building. Error: \{}", sourceName, error.getMessage());
                    return buildContextWithoutPlanning(sourceName);
                });
    }

    private Mono<String> buildContextWithoutPlanning(String sourceName) {
        DocumentIndexingProperties.DocumentIndexingSourceProperties sourceProperties = documentIndexingProperties.getSources().get(sourceName);
        if (sourceProperties == null) {
            throw new AIException(AIErrorCodes.INVALID_DOCUMENT_SOURCE, STR."No indexing properties found for sourceName: \{sourceName}");
        }

        return ragContextBuilder.build(new RagContextRequest(
                sourceProperties.getEmbeddingModel(),
                sourceProperties.getVectorType(),
                new VectorNamespace(sourceProperties.getVectorNamespace(sourceName)),
                "",
                sourceProperties.getTopK(),
                null
        )).map(RagContext::content);
    }

    @Override
    public Mono<DocumentationConversationResponse> startConversation(String sourceName, String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage(sourceName);

        Conversation conversation = new DefaultConversation();
        conversation.addSystemMessage(systemMessage);

        return buildContext(sourceName, model, question)
                .flatMap(context ->
                        conversationClient.chat(conversation, model, question, context)
                                .doOnNext(r -> log.info("LLM response for new conversation: {}", r))
                                .map(chatResponse -> {
                                    UUID conversationId = conversationStore.create(conversation);
                                    return new DocumentationConversationResponse(conversationId, chatResponse);
                                })
                );
    }

    @Override
    public Mono<ChatResponse> ask(UUID conversationId, String sourceName, String model, String question) {
        validateInput(model, question);
        Conversation conversation = conversationStore.get(conversationId);
        return buildContext(sourceName, model, question)
                .flatMap(context -> conversationClient.chat(conversation, model, question, context)
                        .doOnNext(r -> log.info("LLM response for conversationId {}: {}", conversationId, r))
                );
    }

    @Override
    public Mono<ChatResponse> ask(String sourceName, String model, String question) {
        validateInput(model, question);
        String systemMessage = buildSystemMessage(sourceName);
        return buildContext(sourceName, model, question).flatMap(context -> {
            ChatRequest chatRequest = new DefaultChatRequest(model);
            chatRequest.addMessage(DefaultAIMessage.system(systemMessage));
            chatRequest.addMessage(DefaultAIMessage.system(context));
            chatRequest.addMessage(DefaultAIMessage.user(question));
            return chatClient.generate(chatRequest)
                    .doOnNext(r -> log.info("LLM response: {}", r));
        });
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
