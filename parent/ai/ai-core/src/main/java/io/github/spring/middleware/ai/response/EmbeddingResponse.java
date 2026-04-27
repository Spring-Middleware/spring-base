package io.github.spring.middleware.ai.response;

import java.util.List;

public interface EmbeddingResponse extends AIResponse {

    List<Float> getEmbedding();

}
