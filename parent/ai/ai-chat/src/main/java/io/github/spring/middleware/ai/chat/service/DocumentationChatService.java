package io.github.spring.middleware.ai.chat.service;


import io.github.spring.middleware.ai.chat.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.response.ChatResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DocumentationChatService {

    Mono<DocumentationConversationResponse> startConversation(String sourceName, String model, String question);

    Mono<ChatResponse> ask(UUID conversationId, String sourceName, String model, String question);

    Mono<ChatResponse> ask(String sourceName, String model, String question);

}
