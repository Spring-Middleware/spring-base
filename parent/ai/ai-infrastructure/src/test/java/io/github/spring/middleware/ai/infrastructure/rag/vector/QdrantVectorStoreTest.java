package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import io.github.spring.middleware.ai.rag.vector.VectorStore.MatchType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QdrantVectorStoreTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private QdrantVectorStore vectorStore;

    @Test
    void testGetType() {
        assertThat(vectorStore.getType()).isEqualTo(VectorType.QDRANT);
    }

    @Test
    void testAdd() {
        VectorNamespace namespace = new VectorNamespace("my-collection");
        DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID(),
                "doc-1",
                "title",
                "content",
                List.of(1.0f, 2.0f),
                "model",
                Map.of(),
                "checksum",
                java.time.Instant.now()
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/collections/{collection}", namespace.value())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        when(webClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/collections/{collection}/points", namespace.value())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        vectorStore.add(namespace, chunk).block();

        verify(webClient).get();
        verify(webClient).put();
    }

    @Test
    void testSearch() {
        VectorNamespace namespace = new VectorNamespace("my-collection");
        List<Float> embedding = List.of(1.0f, 2.0f);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/collections/{collection}/points/search", namespace.value())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        Map<String, Object> point = Map.of(
                "id", UUID.randomUUID().toString(),
                "payload", Map.of(
                        "documentId", "doc-1",
                        "title", "title",
                        "content", "content",
                        "embeddingModel", "model",
                        "checksum", "checksum",
                        "indexedAt", java.time.Instant.now().toString()
                ),
                "vector", List.of(1.0f, 2.0f)
        );
        Map<String, Object> response = Map.of("result", List.of(point));
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(response));

        List<DocumentChunk> results = vectorStore.search(new SearchRequest(namespace, embedding, 10)).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void testExists() {
        VectorNamespace namespace = new VectorNamespace("my-collection");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/collections/{collection}/points/scroll", namespace.value())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        Map<String, Object> point = Map.of("id", UUID.randomUUID().toString());
        Map<String, Object> result = Map.of("points", List.of(point));
        Map<String, Object> response = Map.of("result", result);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(response));

        boolean exists = vectorStore.exists(namespace, "doc-1", "model", "checksum").block();

        assertThat(exists).isTrue();
    }

    @Test
    void testDeleteByDocumentIdAndEmbeddingModelExceptChecksums() {
        VectorNamespace namespace = new VectorNamespace("my-collection");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/collections/{collection}/points/delete", namespace.value())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        vectorStore.deleteByDocumentIdAndEmbeddingModelExceptChecksums(namespace, "doc-1", "model", Set.of("checksum1")).block();

        verify(webClient).post();
    }

    @Test
    void testSearchByMataDataFieldValue() {
        VectorNamespace namespace = new VectorNamespace("my-collection");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/collections/{collection}/points/scroll", namespace.value())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        Map<String, Object> point = Map.of(
                "id", UUID.randomUUID().toString(),
                "payload", Map.of(
                        "documentId", "doc-1",
                        "title", "title",
                        "content", "content",
                        "embeddingModel", "model",
                        "checksum", "checksum",
                        "indexedAt", java.time.Instant.now().toString()
                )
        );
        Map<String, Object> response = Map.of("result", Map.of("points", List.of(point)));
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(response));

        SearchRequest req = SearchRequest.builder()
                .namespace(namespace)
                .topK(10)
                .filterField("category")
                .filterValues(List.of("tech", "science"))
                .matchType(MatchType.MATCH_ANY)
                .build();
                
        List<DocumentChunk> results = vectorStore.search(req).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }
}
