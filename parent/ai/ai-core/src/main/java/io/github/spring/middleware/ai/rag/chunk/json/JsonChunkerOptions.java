package io.github.spring.middleware.ai.rag.chunk.json;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;

public record JsonChunkerOptions(
        String rulesPath
) implements ChunkerOptions {
}
