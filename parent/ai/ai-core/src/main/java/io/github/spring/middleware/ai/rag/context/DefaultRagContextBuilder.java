package io.github.spring.middleware.ai.rag.context;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import io.github.spring.middleware.ai.request.DefaultEmbeddingRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DefaultRagContextBuilder implements RagContextBuilder {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreRegistry vectorStoreRegistry;

    @Override
    public RagContext build(RagContextRequest request) {
        List<Float> embedding = embeddingClient.generate(
                new DefaultEmbeddingRequest(request.embeddingModel(), request.query())
        ).getEmbedding();

        VectorStore vectorStore = vectorStoreRegistry.findByType(request.vectorType());

        List<DocumentChunk> chunks = vectorStore.search(request.vectorNamespace(), embedding, request.topK());

        String content = chunks.stream()
                .map(this::formatChunk)
                .collect(Collectors.joining("\n\n---\n\n"));

        return new RagContext(content, chunks);
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
