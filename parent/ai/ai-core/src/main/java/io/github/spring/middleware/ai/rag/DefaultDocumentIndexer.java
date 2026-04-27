package io.github.spring.middleware.ai.rag;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.config.DocumentChunkerProperties;
import io.github.spring.middleware.ai.request.DefaultEmbeddingRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class DefaultDocumentIndexer implements DocumentIndexer {

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final DocumentChunker documentChunker;
    private final DocumentChunkerProperties properties;

    public DefaultDocumentIndexer(
            EmbeddingClient embeddingClient,
            VectorStore vectorStore,
            DocumentChunker documentChunker,
            DocumentChunkerProperties properties
    ) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.documentChunker = documentChunker;
        this.properties = properties;
    }

    @Override
    public Mono<Void> index(DocumentSource source, String embeddingModel) {
        return documentChunker.chunk(source, properties)
                .concatMap(chunk ->
                        Mono.fromCallable(() -> embeddingClient.generate(
                                        new DefaultEmbeddingRequest(embeddingModel, chunk.content())
                                ))
                                .map(response -> new DocumentChunk(
                                        UUID.randomUUID(),
                                        source.documentId(),
                                        source.title(),
                                        chunk.content(),
                                        response.getEmbedding(),
                                        chunk.metadata()
                                ))
                                .doOnNext(vectorStore::add)
                )
                .then();
    }
}


