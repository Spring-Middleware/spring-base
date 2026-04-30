package io.github.spring.middleware.ai.rag.chunk.json;

import java.util.List;

public record JsonChunkExtractorRuleResult(List<JsonDataType> jsonDataTypes, String name, Object result) {
}
