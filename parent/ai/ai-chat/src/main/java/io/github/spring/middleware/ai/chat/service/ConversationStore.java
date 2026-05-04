package io.github.spring.middleware.ai.chat.service;

import io.github.spring.middleware.ai.conversation.Conversation;

import java.util.UUID;

public interface ConversationStore {

    UUID create(Conversation conversation);

    Conversation get(UUID conversationId);

    void remove(UUID conversationId);
}
