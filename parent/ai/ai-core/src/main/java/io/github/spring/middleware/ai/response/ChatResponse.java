package io.github.spring.middleware.ai.response;

import io.github.spring.middleware.ai.message.AIMessage;

public interface ChatResponse extends AIResponse {

    AIMessage getMessage();

}
