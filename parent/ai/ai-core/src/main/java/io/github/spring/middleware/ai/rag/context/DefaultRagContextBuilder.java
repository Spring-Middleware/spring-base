package io.github.spring.middleware.ai.rag.context;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.planner.MetadataFilter;
import io.github.spring.middleware.ai.rag.vector.SearchRequest;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import io.github.spring.middleware.ai.request.DefaultEmbeddingRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DefaultRagContextBuilder implements RagContextBuilder {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreRegistry vectorStoreRegistry;

    @Override
    public Mono<RagContext> build(RagContextRequest request) {
        return getContext(request)
                .collectList()
                .map(chunks -> {
                    String content = chunks.stream()
                            .map(this::formatChunk)
                            .collect(Collectors.joining("\n\n---\n\n"));

                    return new RagContext(content, chunks);
                });
    }

    private Flux<DocumentChunk> getContext(RagContextRequest request) {
        VectorStore vectorStore = vectorStoreRegistry.findByType(request.vectorType());

        return buildEmbedding(request)
                .map(embedding -> buildSearchRequest(request, embedding))
                .switchIfEmpty(Mono.fromSupplier(() -> buildSearchRequest(request, null)))
                .flatMapMany(vectorStore::search);
    }

    private Mono<List<Float>> buildEmbedding(RagContextRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return Mono.empty();
        }

        return embeddingClient.generate(
                new DefaultEmbeddingRequest(
                        request.embeddingModel(),
                        request.query()
                )
        ).map(embeddingResponse -> embeddingResponse.getEmbedding());
    }

    private SearchRequest buildSearchRequest(
            RagContextRequest request,
            List<Float> embedding
    ) {
        SearchRequest.SearchRequestBuilder searchBuilder = SearchRequest.builder()
                .namespace(request.vectorNamespace())
                .topK(request.topK());

        if (embedding != null && !embedding.isEmpty()) {
            searchBuilder.embedding(embedding);
        }

        if (request.metadataFilters() != null && !request.metadataFilters().isEmpty()) {
            MetadataFilter mf = request.metadataFilters().getFirst();

            searchBuilder.filterField(mf.field())
                    .filterValues(mf.values())
                    .matchType(mf.matchType());
        }

        return searchBuilder.build();
    }


    private String formatChunk(DocumentChunk chunk) {
        return """
                [Document]
                title: %s
                documentId: %s
                source: %s
                
                %s
                """.formatted(
                chunk.title(),
                chunk.documentId(),
                chunk.metadata().getOrDefault("source", "unknown"),
                chunk.content()
        );
    }
}
