package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.fieldAny;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.fieldMatch;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.match;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.must;


@Slf4j
@RequiredArgsConstructor
public class QdrantVectorStore implements VectorStore {

    private final WebClient webClient;

    @Override
    public void add(VectorNamespace namespace, DocumentChunk chunk) {
        String collection = namespace.value();

        // 1. Ensure collection exists (dimensión del embedding)
        ensureCollection(collection, chunk.embedding().size());

        // 2. Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentId", chunk.documentId());
        payload.put("embeddingModel", chunk.embeddingModel());
        payload.put("checksum", chunk.checksum());
        payload.put("content", chunk.content());
        payload.put("metadata", chunk.metadata());

        // 3. Build point
        Map<String, Object> point = Map.of(
                "id", chunk.id() != null ? chunk.id().toString() : UUID.randomUUID().toString(),
                "vector", chunk.embedding(),
                "payload", payload
        );

        Map<String, Object> body = Map.of(
                "points", List.of(point)
        );

        // 4. Upsert
        webClient.put()
                .uri("/collections/{collection}/points", collection)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public List<DocumentChunk> search(VectorNamespace namespace, List<Float> embedding, int topK) {
        Map<String, Object> body = Map.of(
                "vector", embedding,
                "limit", topK,
                "with_payload", true,
                "with_vector", true
        );

        Map<String, Object> response = webClient.post()
                .uri("/collections/{collection}/points/search", namespace.value())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

        if (response == null || response.get("result") == null) {
            return List.of();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");

        return results.stream()
                .map(this::toDocumentChunk)
                .toList();
    }

    @Override
    public boolean exists(VectorNamespace namespace, String documentId, String embeddingModel, String checksum) {
        Map<String, Object> body = Map.of(
                "filter", must(
                        match("documentId", documentId),
                        match("embeddingModel", embeddingModel),
                        match("checksum", checksum)
                ),
                "limit", 1,
                "with_payload", false,
                "with_vector", false
        );

        try {

            Map<String, Object> response = webClient.post()
                    .uri("/collections/{collection}/points/scroll", namespace.value())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (response == null || response.get("result") == null) {
                return false;
            }

            Map<String, Object> result = (Map<String, Object>) response.get("result");
            List<?> points = (List<?>) result.get("points");

            return points != null && !points.isEmpty();
        } catch (WebClientResponseException.NotFound e) {
            return false;
        }
    }

    @Override
    public void deleteByDocumentIdAndEmbeddingModelExceptChecksums(VectorNamespace namespace, String documentId, String embeddingModel, Set<String> checksums) {

        Map<String, Object> filter = checksums == null || checksums.isEmpty()
                ? Map.of(
                "must", List.of(
                        fieldMatch("documentId", documentId),
                        fieldMatch("embeddingModel", embeddingModel)
                )
        )
                : Map.of(
                "must", List.of(
                        fieldMatch("documentId", documentId),
                        fieldMatch("embeddingModel", embeddingModel)
                ),
                "must_not", List.of(
                        fieldAny("checksum", checksums)
                )
        );

        Map<String, Object> body = Map.of(
                "filter", filter
        );

        try {
            webClient.post()
                    .uri("/collections/{collection}/points/delete", namespace.value())
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException.NotFound ignored) {
            log.warn("Collection {} not found when trying to delete points. Ignoring.", namespace.value());
        }
    }

    @Override
    public VectorType getType() {
        return VectorType.QDRANT;
    }

    @SuppressWarnings("unchecked")
    private DocumentChunk toDocumentChunk(Map<String, Object> point) {

        // 1. ID (nivel root, no payload)
        UUID id = UUID.fromString((String) point.get("id"));

        // 2. Payload
        Map<String, Object> payload = (Map<String, Object>) point.get("payload");

        // 3. Vector (puede venir null si no pides with_vector)
        List<Float> vector = point.get("vector") != null
                ? ((List<?>) point.get("vector"))
                .stream()
                .map(v -> ((Number) v).floatValue())
                .toList()
                : List.of();

        return new DocumentChunk(
                id,
                (String) payload.get("documentId"),
                (String) payload.get("title"),
                (String) payload.get("content"),
                vector,
                (String) payload.get("embeddingModel"),
                (Map<String, Object>) payload.getOrDefault("metadata", Map.of()),
                (String) payload.get("checksum"),
                payload.get("indexedAt") != null
                        ? Instant.parse((String) payload.get("indexedAt"))
                        : Instant.now()
        );
    }


    private void ensureCollection(String collection, int dimension) {

        try {
            webClient.get()
                    .uri("/collections/{collection}", collection)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException.NotFound e) {

            Map<String, Object> vectors = Map.of(
                    "size", dimension,
                    "distance", "Cosine"
            );

            Map<String, Object> body = Map.of(
                    "vectors", vectors
            );

            webClient.put()
                    .uri("/collections/{collection}", collection)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }
    }

}
