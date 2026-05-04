package io.github.spring.middleware.ai.chat.domain;

import io.github.spring.middleware.ai.response.ChatResponse;

import java.util.UUID;

public record DocumentationConversationResponse(UUID conversationId, ChatResponse response) {
}
