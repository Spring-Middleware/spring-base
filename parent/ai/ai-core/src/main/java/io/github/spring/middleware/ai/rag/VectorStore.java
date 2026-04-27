package io.github.spring.middleware.ai.rag;

import java.util.List;

public interface VectorStore {

    void add(DocumentChunk chunk);

    List<DocumentChunk> search(List<Float> embedding, int topK);

}