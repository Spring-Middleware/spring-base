package io.github.spring.middleware.ai.rag.chunk.json;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;

import java.util.List;

public record JsonChunkerOptions(
        List<String> rulesPath
) implements ChunkerOptions {
}
