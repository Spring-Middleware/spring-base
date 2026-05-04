package io.github.spring.middleware.ai.rag.planner;

import io.github.spring.middleware.ai.rag.vector.VectorStore;
import java.util.List;

public record MetadataFilter(
        String field,
        List<String> values,
        VectorStore.MatchType matchType
) {}

