package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.utils.CosineSimilarity;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


public class InMemoryVectorStore implements VectorStore {

    private final Map<String, List<DocumentChunk>> chunks = new HashMap<>();


    @Override
    public Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk) {
        return Mono.fromRunnable(() ->
                chunks.computeIfAbsent(namespace.value(), k -> new CopyOnWriteArrayList<>())
                        .add(chunk)
        );
    }

    @Override
    public Flux<DocumentChunk> search(SearchRequest request) {
        List<DocumentChunk> namespaceChunks = chunks.getOrDefault(request.namespace().value(), List.of());

        if (request.hasFilter()) {
            namespaceChunks = namespaceChunks.stream()
                    .filter(chunk -> matches(chunk.metadata(), request.filterField(), request.filterValues(), request.matchType()))
                    .toList();
        }

        if (request.embedding() != null && !request.embedding().isEmpty()) {
            return Flux.fromIterable(namespaceChunks.stream()
                    .sorted(Comparator.comparingDouble(chunk ->
                            -CosineSimilarity.calculate(request.embedding(), chunk.embedding())
                    ))
                    .limit(request.topK())
                    .toList());
        } else {
            return Flux.fromIterable(namespaceChunks.stream()
                    .limit(request.topK())
                    .toList());
        }
    }

    private boolean matches(
            Map<String, Object> metadata,
            String field,
            List<String> values,
            MatchType matchType
    ) {
        Object fieldValue = getFieldValue(metadata, field);

        if (fieldValue == null) return false;

        List<String> normalizedValues = flatten(fieldValue);

        return switch (matchType) {
            case MATCH_ANY -> values.stream().anyMatch(normalizedValues::contains);

            case MATCH_ALL -> values.stream().allMatch(normalizedValues::contains);
        };
    }

    private Object getFieldValue(Map<String, Object> metadata, String fieldPath) {
        String[] parts = fieldPath.split("\\.");

        Object current = metadata;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) return null;
        }

        return current;
    }

    private List<String> flatten(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .flatMap(v -> flatten(v).stream())
                    .toList();
        }

        if (value instanceof Map<?, ?> map) {
            return map.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(v -> flatten(v).stream())
                    .toList();
        }

        return List.of(String.valueOf(value));
    }


    @Override
    public VectorType getType() {
        return VectorType.IN_MEMORY;
    }

    @Override
    public Mono<Boolean> exists(VectorNamespace namespace,
                             String documentId,
                             String embeddingModel,
                             String checksum) {

        return Mono.fromSupplier(() -> {
            return chunks.getOrDefault(namespace.value(), List.of()).stream()
                    .anyMatch(chunk ->
                            chunk.documentId().equals(documentId) &&
                                    chunk.embeddingModel().equals(embeddingModel) &&
                                    chunk.checksum().equals(checksum)
                    );
        });
    }

    @Override
    public Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(
            VectorNamespace namespace,
            String documentId,
            String embeddingModel,
            Set<String> checksums
    ) {
        return Mono.fromRunnable(() -> {
            List<DocumentChunk> list = chunks.get(namespace.value());
            if (list != null) {
                list.removeIf(chunk ->
                        chunk.documentId().equals(documentId)
                                && embeddingModel.equals(chunk.embeddingModel())
                                && !checksums.contains(chunk.checksum())
                );
            }
        });
    }

}
