package io.github.spring.middleware.ai.request;

public record DocumentationChatRequest(
        String sourceName,
        String model,
        String question
) {}

