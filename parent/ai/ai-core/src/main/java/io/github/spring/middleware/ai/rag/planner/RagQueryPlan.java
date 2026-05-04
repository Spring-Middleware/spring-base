package io.github.spring.middleware.ai.rag.planner;

import java.util.List;

public record RagQueryPlan(
        String optimizedQuery,
        List<MetadataFilter> filters,
        boolean useSemanticSearch
) {

    @Override
    public String toString() {
        return STR."RagQueryPlan{optimizedQuery='\{optimizedQuery}', filters=\{filters}, useSemanticSearch=\{useSemanticSearch}}";
    }
}

