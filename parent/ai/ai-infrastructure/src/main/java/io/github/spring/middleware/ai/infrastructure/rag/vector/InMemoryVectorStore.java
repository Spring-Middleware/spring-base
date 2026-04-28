package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


public class InMemoryVectorStore implements VectorStore {

    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();


    @Override
    public void add(DocumentChunk chunk) {
        chunks.add(chunk);
    }

    @Override
    public List<DocumentChunk> search(List<Float> embedding, int topK) {
        return chunks.stream()
                .sorted(Comparator.comparingDouble(chunk ->
                        -CosineSimilarity.calculate(embedding, chunk.embedding())
                ))
                .limit(topK)
                .toList();
    }

    @Override
    public VectorType getType() {
        return VectorType.IN_MEMORY;
    }

    public boolean exists(String documentId, String embeddingModel, String checksum) {
        return chunks.stream().anyMatch(chunk ->
                chunk.documentId().equals(documentId) &&
                        chunk.embeddingModel().equals(embeddingModel) &&
                        chunk.checksum().equals(checksum)
        );
    }

    public void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            String documentId,
            String embeddingModel,
            Set<String> checksums
    ) {
        chunks.removeIf(chunk ->
                chunk.documentId().equals(documentId)
                        && embeddingModel.equals(chunk.embeddingModel())
                        && !checksums.contains(chunk.checksum())
        );
    }

}
