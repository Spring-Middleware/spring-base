package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore.MatchType;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorStoreTest {

    private InMemoryVectorStore vectorStore;
    private final VectorNamespace namespace = new VectorNamespace("test-namespace");

    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryVectorStore();
    }

    @Test
    void testGetType() {
        assertThat(vectorStore.getType()).isEqualTo(VectorType.IN_MEMORY);
    }

    @Test
    void testAddAndExists() {
        DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID(),
                "doc-1",
                "title",
                "content",
                List.of(1.0f, 2.0f),
                "model-a",
                Map.of("key", "value"),
                "checksum123",
                java.time.Instant.now()
        );

        vectorStore.add(namespace, chunk).block();

        assertThat(vectorStore.exists(namespace, "doc-1", "model-a", "checksum123").block()).isTrue();
        assertThat(vectorStore.exists(namespace, "doc-1", "model-a", "wrong-checksum").block()).isFalse();
    }

    @Test
    void testSearch() {
        DocumentChunk chunk1 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(1.0f, 0.0f), "model", Map.of(), "chk1", java.time.Instant.now());
        DocumentChunk chunk2 = new DocumentChunk(UUID.randomUUID(), "doc-2", "", "", List.of(0.0f, 1.0f), "model", Map.of(), "chk2", java.time.Instant.now());

        vectorStore.add(namespace, chunk1).block();
        vectorStore.add(namespace, chunk2).block();

        // Vector closer to chunk1
        List<DocumentChunk> results = vectorStore.search(new SearchRequest(namespace, List.of(0.9f, 0.1f), 1)).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void testDeleteByDocumentIdAndEmbeddingModelExceptChecksums() {
        DocumentChunk chunk1 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(), "model-a", Map.of(), "chk-keep", java.time.Instant.now());
        DocumentChunk chunk2 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(), "model-a", Map.of(), "chk-discard", java.time.Instant.now());
        DocumentChunk chunk3 = new DocumentChunk(UUID.randomUUID(), "doc-2", "", "", List.of(), "model-a", Map.of(), "chk-discard", java.time.Instant.now());

        vectorStore.add(namespace, chunk1).block();
        vectorStore.add(namespace, chunk2).block();
        vectorStore.add(namespace, chunk3).block();

        vectorStore.deleteByDocumentIdAndEmbeddingModelExceptChecksums(namespace, "doc-1", "model-a", Set.of("chk-keep")).block();

        assertThat(vectorStore.exists(namespace, "doc-1", "model-a", "chk-keep").block()).isTrue();
        assertThat(vectorStore.exists(namespace, "doc-1", "model-a", "chk-discard").block()).isFalse();
        assertThat(vectorStore.exists(namespace, "doc-2", "model-a", "chk-discard").block()).isTrue(); // Unaffected documentId
    }

    @Test
    void testSearchByMataDataFieldValue() {
        DocumentChunk chunk1 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(), "model", Map.of("category", "tech"), "chk1", java.time.Instant.now());
        DocumentChunk chunk2 = new DocumentChunk(UUID.randomUUID(), "doc-2", "", "", List.of(), "model", Map.of("category", "science"), "chk2", java.time.Instant.now());

        vectorStore.add(namespace, chunk1).block();
        vectorStore.add(namespace, chunk2).block();

        SearchRequest req = SearchRequest.builder()
                .namespace(namespace)
                .topK(10)
                .filterField("category")
                .filterValues(List.of("tech", "unknown"))
                .matchType(MatchType.MATCH_ANY)
                .build();

        List<DocumentChunk> results = vectorStore.search(req).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }
}
