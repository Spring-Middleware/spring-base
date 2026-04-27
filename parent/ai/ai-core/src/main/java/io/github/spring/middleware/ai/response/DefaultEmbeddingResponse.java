package io.github.spring.middleware.ai.response;

import java.util.List;

public class DefaultEmbeddingResponse implements EmbeddingResponse {

    private final List<Float> embedding;

    public DefaultEmbeddingResponse(List<Float> embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException("embedding must not be null");
        }
        this.embedding = embedding;
    }

    @Override
    public List<Float> getEmbedding() {
        return embedding;
    }
}
