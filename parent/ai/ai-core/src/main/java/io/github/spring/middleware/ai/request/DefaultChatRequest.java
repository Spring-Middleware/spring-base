package io.github.spring.middleware.ai.request;

import io.github.spring.middleware.ai.message.AIMessage;

import java.util.LinkedList;
import java.util.List;

public class DefaultChatRequest implements ChatRequest {

    private final String model;
    private List<AIMessage> messages = new LinkedList<>();

    public DefaultChatRequest(String model) {
        this.model = model;
    }

    @Override
    public List<AIMessage> getMessages() {
        return List.copyOf(messages);
    }

    public void addMessage(AIMessage message) {
        this.messages.add(message);
    }

    @Override
    public String getModel() {
        return this.model;
    }


    @Override
    public String toString() {
        return STR."DefaultChatRequest{model='\{model}', messages=\{messages}}";
    }
}
