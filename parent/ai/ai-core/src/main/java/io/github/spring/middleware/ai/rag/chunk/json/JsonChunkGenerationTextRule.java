package io.github.spring.middleware.ai.rag.chunk.json;

import java.util.Map;

public record JsonChunkGenerationTextRule(String template, Map<String, String> variables) {
}
