package io.github.spring.middleware.ai.infrastructure.rag.vector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QdrantUtils {

    private QdrantUtils() {
    }

    public static Map<String, Object> must(Map<String, Object>... conditions) {
        return Map.of("must", List.of(conditions));
    }

    public static Map<String, Object> match(String field, Object value) {
        return Map.of(
                "key", field,
                "match", Map.of("value", value)
        );
    }

    public static Map<String, Object> fieldMatch(String field, Object value) {
        return Map.of(
                "key", field,
                "match", Map.of("value", value)
        );
    }

    public static Map<String, Object> fieldAny(String field, Collection<String> values) {
        return Map.of(
                "key", field,
                "match", Map.of("any", values)
        );
    }

}
