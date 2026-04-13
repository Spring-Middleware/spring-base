package io.github.spring.middleware.ai.conversation;

import io.github.spring.middleware.ai.message.AIMessage;
import io.github.spring.middleware.ai.request.ChatRequest;

import java.util.List;

public interface Conversation {

    /**
     * Returns the ordered list of messages in the conversation.
     */
    List<AIMessage> getMessages();

    /**
     * Adds a message to the conversation.
     */
    void addMessage(AIMessage message);

    /**
     * Adds a system message.
     */
    void addSystemMessage(String content);

    /**
     * Adds a user message.
     */
    void addUserMessage(String content);

    /**
     * Adds an assistant message.
     */
    void addAssistantMessage(String content);

    /**
     * Returns true if the conversation has no messages.
     */
    boolean isEmpty();

    /**
     * Returns the number of messages in the conversation.
     */
    int size();

    /**
     * Builds a ChatRequest from the current conversation state.
     */
    ChatRequest toRequest(String model);

}
