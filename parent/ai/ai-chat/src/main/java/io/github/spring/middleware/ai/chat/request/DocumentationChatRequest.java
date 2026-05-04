package io.github.spring.middleware.ai.chat.request;

public record DocumentationChatRequest(
        String sourceName,
        String model,
        String question
) {}

