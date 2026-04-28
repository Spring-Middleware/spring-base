package io.github.spring.middleware.ai.request;

import io.github.spring.middleware.ai.message.AIMessage;

import java.util.List;

public interface ChatRequest extends AIRequest {

    List<AIMessage> getMessages();

    void addMessage(AIMessage message);

}
