package io.github.spring.middleware.ai.service.impl;

import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.exception.ConversationNotFondException;
import io.github.spring.middleware.ai.service.ConversationStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryConversationStore implements ConversationStore {

    private final Map<UUID, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public UUID create(Conversation conversation) {
        UUID id = UUID.randomUUID();
        conversations.put(id, conversation);
        return id;
    }

    @Override
    public Conversation get(UUID conversationId) {
        Conversation conversation = conversations.get(conversationId);

        if (conversation == null) {
            throw new ConversationNotFondException(STR."Conversation not found: \{conversationId}"
            );
        }

        return conversation;
    }

    @Override
    public void remove(UUID conversationId) {
        conversations.remove(conversationId);
    }
}
