package io.github.spring.middleware.ai.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public interface VectorStore {

    Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk);

    Flux<DocumentChunk> search(SearchRequest request);

    Mono<Boolean> exists(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            String checksum
    );

    Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            Set<String> checksums
    );

    VectorType getType();

    enum MatchType {
        MATCH_ALL,
        MATCH_ANY;
    }
}