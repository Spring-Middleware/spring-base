package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.utils.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


public class InMemoryVectorStore implements VectorStore {

    private final Map<String, List<DocumentChunk>> chunks = new HashMap<>();


    @Override
    public void add(VectorNamespace namespace, DocumentChunk chunk) {
        chunks.computeIfAbsent(namespace.value(), k -> new CopyOnWriteArrayList<>()).add(chunk);
    }

    @Override
    public List<DocumentChunk> search(VectorNamespace namespace, List<Float> embedding, int topK) {
        return chunks.getOrDefault(namespace.value(), List.of()).stream()
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

    @Override
    public boolean exists(VectorNamespace namespace, String documentId, String embeddingModel, String checksum) {
        return chunks.getOrDefault(namespace.value(), List.of()).stream()
                .anyMatch(chunk ->
                chunk.documentId().equals(documentId) &&
                        chunk.embeddingModel().equals(embeddingModel) &&
                        chunk.checksum().equals(checksum)
        );
    }

    @Override
    public void deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            Set<String> checksums
    ) {
        List<DocumentChunk> list = chunks.get(namespace.value());
        if (list != null) {
            list.removeIf(chunk ->
                chunk.documentId().equals(documentId)
                        && embeddingModel.equals(chunk.embeddingModel())
                        && !checksums.contains(chunk.checksum())
            );
        }
    }

}
