package io.github.spring.middleware.ai.rag.chunk.json;

import java.util.List;

public record JsonChunkExtractorRule(List<JsonDataType> jsonDataTypes, String name, String  extractorPath) {
}
