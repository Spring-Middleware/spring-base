package io.github.spring.middleware.ai.rag.planner;

public record SelfQueryRequest(
        String sourceName,
        String model,
        String plannerContext,
        String query,
        String chunker
) {}
