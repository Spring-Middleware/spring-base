package io.github.spring.middleware.ai.conversation.client;

import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.response.ChatResponse;
import reactor.core.publisher.Mono;

public interface ConversationClient {

    Mono<ChatResponse> chat(Conversation conversation, String model, String userMessage, String context);

}