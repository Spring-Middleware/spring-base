package io.github.spring.middleware.ai.rag.vector;

import java.util.List;

public record SearchRequest(
        VectorNamespace namespace,
        List<Float> embedding,
        int topK,
        String filterField,
        List<String> filterValues,
        VectorStore.MatchType matchType
) {
    public SearchRequest(VectorNamespace namespace, List<Float> embedding, int topK) {
        this(namespace, embedding, topK, null, null, null);
    }

    public static class SearchRequestBuilder {
        private VectorNamespace namespace;
        private List<Float> embedding;
        private int topK;
        private String filterField;
        private List<String> filterValues;
        private VectorStore.MatchType matchType;

        SearchRequestBuilder() {}

        public SearchRequestBuilder namespace(VectorNamespace namespace) {
            this.namespace = namespace;
            return this;
        }
        public SearchRequestBuilder embedding(List<Float> embedding) {
            this.embedding = embedding;
            return this;
        }
        public SearchRequestBuilder topK(int topK) {
            this.topK = topK;
            return this;
        }
        public SearchRequestBuilder filterField(String filterField) {
            this.filterField = filterField;
            return this;
        }
        public SearchRequestBuilder filterValues(List<String> filterValues) {
            this.filterValues = filterValues;
            return this;
        }
        public SearchRequestBuilder matchType(VectorStore.MatchType matchType) {
            this.matchType = matchType;
            return this;
        }
        public SearchRequest build() {
            return new SearchRequest(namespace, embedding, topK, filterField, filterValues, matchType);
        }
    }

    public static SearchRequestBuilder builder() {
        return new SearchRequestBuilder();
    }

    public boolean hasFilter() {
        return filterField != null && !filterField.isBlank() &&
               filterValues != null && !filterValues.isEmpty() &&
               matchType != null;
    }
}
