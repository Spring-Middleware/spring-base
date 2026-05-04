package io.github.spring.middleware.ai.infrastructure.rag.vector;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorNamespace;
import io.github.spring.middleware.ai.rag.vector.VectorStore.MatchType;
import io.github.spring.middleware.ai.rag.vector.VectorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoVectorStoreTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MongoVectorStore vectorStore;

    private final VectorNamespace namespace = new VectorNamespace("my-mongo-collection");

    @Test
    void testGetType() {
        assertThat(vectorStore.getType()).isEqualTo(VectorType.MONGO);
    }

    @Test
    void testAdd() {
        DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID(), "doc-1", "title", "content",
                List.of(1.0f, 2.0f), "model", Map.of(), "checksum", java.time.Instant.now()
        );

        vectorStore.add(namespace, chunk).block();

        verify(mongoTemplate).save(chunk, namespace.value());
    }

    @Test
    void testSearch() {
        DocumentChunk chunk1 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(1.0f, 0.0f), "model", Map.of(), "chk", java.time.Instant.now());
        when(mongoTemplate.find(any(Query.class), eq(DocumentChunk.class), eq(namespace.value()))).thenReturn(List.of(chunk1));

        List<DocumentChunk> results = vectorStore.search(new SearchRequest(namespace, List.of(0.9f, 0.1f), 10)).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void testExists() {
        when(mongoTemplate.exists(any(Query.class), eq(DocumentChunk.class), eq(namespace.value()))).thenReturn(true);

        boolean exists = Boolean.TRUE.equals(vectorStore.exists(namespace, "doc-1", "model", "checksum").block());

        assertThat(exists).isTrue();
    }

    @Test
    void testDeleteByDocumentIdAndEmbeddingModelExceptChecksums() {
        vectorStore.deleteByDocumentIdAndEmbeddingModelExceptChecksums(namespace, "doc-1", "model", Set.of("chk1")).block();

        verify(mongoTemplate).remove(any(Query.class), eq(DocumentChunk.class), eq(namespace.value()));
    }

    @Test
    void testSearchByMataDataFieldValue() {
        DocumentChunk chunk1 = new DocumentChunk(UUID.randomUUID(), "doc-1", "", "", List.of(), "model", Map.of("category", "tech"), "chk1", java.time.Instant.now());
        when(mongoTemplate.find(any(Query.class), eq(DocumentChunk.class), eq(namespace.value()))).thenReturn(List.of(chunk1));

        SearchRequest req = SearchRequest.builder()
                .namespace(namespace)
                .topK(10)
                .filterField("category")
                .filterValues(List.of("tech"))
                .matchType(MatchType.MATCH_ANY)
                .build();
                
        List<DocumentChunk> results = vectorStore.search(req).collectList().block();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).documentId()).isEqualTo("doc-1");
    }
}
