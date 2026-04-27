package io.github.spring.middleware.ai.request;

public record DocumentationChatRequest(
        String model,
        String question
) {}

