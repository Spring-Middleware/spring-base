package io.github.spring.middleware.ai.conversation;

import io.github.spring.middleware.ai.message.AIMessage;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.request.DefaultChatRequest;

import java.util.LinkedList;
import java.util.List;

public class DefaultConversation implements Conversation {

    private final List<AIMessage> messages = new LinkedList<>();

    @Override
    public List<AIMessage> getMessages() {
        return List.copyOf(messages);
    }

    @Override
    public void addMessage(AIMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        this.messages.add(message);
    }

    @Override
    public void addSystemMessage(String content) {
        this.messages.add(DefaultAIMessage.system(content));
    }

    @Override
    public void addUserMessage(String content) {
        this.messages.add(DefaultAIMessage.user(content));
    }

    @Override
    public void addAssistantMessage(String content) {
        this.messages.add(DefaultAIMessage.assistant(content));
    }

    @Override
    public boolean isEmpty() {
        return this.messages.isEmpty();
    }

    @Override
    public int size() {
        return this.messages.size();
    }

    @Override
    public ChatRequest toRequest(String model) {
        DefaultChatRequest request = new DefaultChatRequest(model);
        this.messages.forEach(request::addMessage);
        return request;
    }

    @Override
    public String toString() {
        return "DefaultConversation{" +
                "messages=" + messages +
                '}';
    }
}