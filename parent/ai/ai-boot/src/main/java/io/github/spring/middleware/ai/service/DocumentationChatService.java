package io.github.spring.middleware.ai.service;

import io.github.spring.middleware.ai.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.response.ChatResponse;

import java.util.UUID;

public interface DocumentationChatService {

    DocumentationConversationResponse startConversation(String sourceName, String model, String question);

    ChatResponse ask(UUID conversationId, String sourceName, String model, String question);

}
