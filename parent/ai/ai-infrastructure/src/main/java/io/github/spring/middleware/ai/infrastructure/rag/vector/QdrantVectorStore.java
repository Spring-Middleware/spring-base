package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.fieldAny;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.fieldMatch;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.match;
import static io.github.spring.middleware.ai.infrastructure.rag.vector.QdrantUtils.must;


@Slf4j
@RequiredArgsConstructor
public class QdrantVectorStore implements VectorStore {

    private final WebClient webClient;

    private final ConcurrentMap<String, Mono<Void>> collectionCreationCache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> add(VectorNamespace namespace, DocumentChunk chunk) {
        return ensureCollectionOnce(namespace.value(), chunk.embedding().size())
                .then(upsertPoint(namespace.value(), chunk));
    }

    private Mono<Void> upsertPoint(String collection, DocumentChunk chunk) {

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
        return webClient.put()
                .uri("/collections/{collection}/points", collection)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @Override
    public Flux<DocumentChunk> search(SearchRequest request) {
        Map<String, Object> filter = null;

        if (request.hasFilter()) {
            List<String> values = request.filterValues().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .distinct()
                    .toList();
            if (!values.isEmpty()) {
                String qdrantField = request.filterField().startsWith("metadata.")
                        ? request.filterField()
                        : STR."metadata.\{request.filterField()}";

                filter = switch (request.matchType()) {
                    case MATCH_ANY -> Map.of(
                            "must", List.of(
                                    Map.of(
                                            "key", qdrantField,
                                            "match", Map.of("any", values)
                                    )
                            )
                    );
                    case MATCH_ALL -> Map.of(
                            "must", values.stream()
                                    .map(v -> Map.of(
                                            "key", qdrantField,
                                            "match", Map.of("value", v)
                                    ))
                                    .toList()
                    );
                };
            }
        }

        Map<String, Object> body = new HashMap<>();

        String uriPath;
        if (request.embedding() != null && !request.embedding().isEmpty()) {
            body.put("vector", request.embedding());
            body.put("limit", request.topK());
            body.put("with_payload", true);
            body.put("with_vector", true);
            if (filter != null) {
                body.put("filter", filter);
            }
            uriPath = "/collections/{collection}/points/search";
        } else {
            if (filter != null) {
                body.put("filter", filter);
            }
            body.put("limit", request.topK());
            body.put("with_payload", true);
            body.put("with_vector", false); // maybe we need vector? default false when just scrolling
            uriPath = "/collections/{collection}/points/scroll";
        }

        log.debug("Searching in Qdrant collection '{}' with body: {}", request.namespace().value(), body);

        return webClient.post()
                .uri(uriPath, request.namespace().value())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMapMany(response -> {
                    if (response == null || response.get("result") == null) {
                        return Flux.empty();
                    }

                    List<Map<String, Object>> results;

                    if (request.embedding() != null && !request.embedding().isEmpty()) {
                        results = (List<Map<String, Object>>) response.get("result");
                    } else {
                        Map<String, Object> resultObj =
                                (Map<String, Object>) response.get("result");
                        results = (List<Map<String, Object>>) resultObj.get("points");
                    }

                    if (results == null || results.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(results)
                            .map(this::toDocumentChunk);
                });
    }

    @Override
    public Mono<Boolean> exists(VectorNamespace namespace,
                                String documentId,
                                String embeddingModel,
                                String checksum) {

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

        return webClient.post()
                .uri("/collections/{collection}/points/scroll", namespace.value())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> {
                    if (response == null || response.get("result") == null) {
                        return false;
                    }

                    Map<String, Object> result = (Map<String, Object>) response.get("result");
                    List<?> points = (List<?>) result.get("points");

                    return points != null && !points.isEmpty();
                })
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.just(false));
    }

    @Override
    public Mono<Void> deleteByDocumentIdAndEmbeddingModelExceptChecksums(VectorNamespace namespace, String documentId, String embeddingModel, Set<String> checksums) {

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

        return webClient.post()
                .uri("/collections/{collection}/points/delete", namespace.value())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .then();

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

    private Mono<Void> ensureCollectionOnce(String collection, int dimension) {
        return collectionCreationCache.computeIfAbsent(collection, key ->
                ensureCollection(collection, dimension).cache()
        );
    }


    private Mono<Void> ensureCollection(String collection, int dimension) {
        return webClient.get()
                .uri("/collections/{collection}", collection)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.NotFound.class, e -> createCollection(collection, dimension));
    }

    private Mono<Void> createCollection(String collection, int dimension) {
        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", dimension,
                        "distance", "Cosine"
                )
        );

        return webClient.put()
                .uri("/collections/{collection}", collection)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .then()
                // por si hay carrera y otro hilo la crea antes
                .onErrorResume(WebClientResponseException.Conflict.class, e -> Mono.empty());
    }

}
