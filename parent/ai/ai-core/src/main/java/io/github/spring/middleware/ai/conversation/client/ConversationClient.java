package io.github.spring.middleware.ai.conversation.client;

import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.response.ChatResponse;

public interface ConversationClient {

    ChatResponse chat(Conversation conversation, String model, String userMessage, String context);

}