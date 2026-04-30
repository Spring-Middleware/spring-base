package io.github.spring.middleware.ai.rag.chunk.json;

import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public record JsonChunkRulesDefinition(List<JsonChunkRule> rules) implements ChunkerOptions {
}
