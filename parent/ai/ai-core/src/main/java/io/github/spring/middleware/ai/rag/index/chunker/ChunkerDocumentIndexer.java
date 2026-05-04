package io.github.spring.middleware.ai.rag.index.chunker;

import io.github.spring.middleware.ai.client.EmbeddingClient;
import io.github.spring.middleware.ai.rag.chunk.ChunkerOptions;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunk;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.registry.DocumentChunkerRegistry;
import io.github.spring.middleware.ai.rag.index.DocumentIndexer;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerType;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.utils.ChecksumUtils;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.rag.vector.VectorStoreRegistry;
import io.github.spring.middleware.ai.request.DefaultEmbeddingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChunkerDocumentIndexer implements DocumentIndexer<ChunkerDocumentIndexerOptions> {

    private final EmbeddingClient embeddingClient;
    private final VectorStoreRegistry vectorStoreRegistry;
    private final DocumentChunkerRegistry documentChunkerRegistry;

    public ChunkerDocumentIndexer(
            EmbeddingClient embeddingClient,
            VectorStoreRegistry vectorStoreRegistry,
            DocumentChunkerRegistry documentChunkerRegistry
    ) {
        this.embeddingClient = embeddingClient;
        this.vectorStoreRegistry = vectorStoreRegistry;
        this.documentChunkerRegistry = documentChunkerRegistry;
    }

    @Override
    public <O extends ChunkerOptions> Mono<Void> index(String sourceName, DocumentSource source, ChunkerDocumentIndexerOptions documentIndexerOptions) {
        O options = (O) documentIndexerOptions.getChunkOptions();
        VectorStore vectorStore = vectorStoreRegistry.findByType(documentIndexerOptions.getVectorType());

        Set<String> currentChecksums = ConcurrentHashMap.newKeySet();
        DocumentChunker<O> documentChunker =
                (DocumentChunker<O>) documentChunkerRegistry.findByName(documentIndexerOptions.getChunkerName());

        return documentChunker.chunk(source, options)
                .concatMap(chunk -> {
                    Map<String, Object> metadataChecksum = new HashMap<>(chunk.metadata());
                    metadataChecksum.put("embeddingModel", documentIndexerOptions.getEmbeddingModel());

                    String checksum = ChecksumUtils.checksum(
                            chunk.getEmbeddingContent(),
                            metadataChecksum
                    );

                    currentChecksums.add(checksum);

                    return vectorStore.exists(
                                    documentIndexerOptions.getVectorNamespace(),
                                    source.documentId(),
                                    documentIndexerOptions.getEmbeddingModel(),
                                    checksum
                            )
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.<Void>empty();
                                }

                                return embeddingClient.generate(
                                                new DefaultEmbeddingRequest(
                                                        documentIndexerOptions.getEmbeddingModel(),
                                                        chunk.getEmbeddingContent()
                                                )
                                        )
                                        .map(response -> new DocumentChunk(
                                                UUID.randomUUID(),
                                                source.documentId(),
                                                source.title(),
                                                chunk.content(),
                                                response.getEmbedding(),
                                                documentIndexerOptions.getEmbeddingModel(),
                                                chunk.metadata(),
                                                checksum,
                                                Instant.now()
                                        ))
                                        .flatMap(vectorChunk ->
                                                vectorStore.add(
                                                        documentIndexerOptions.getVectorNamespace(),
                                                        vectorChunk
                                                )
                                        );
                            });
                })
                .then(Mono.defer(() ->
                                vectorStore.deleteByDocumentIdAndEmbeddingModelExceptChecksums(
                                        documentIndexerOptions.getVectorNamespace(),
                                        source.documentId(),
                                        documentIndexerOptions.getEmbeddingModel(),
                                        currentChecksums
                                ))
                        .doOnSuccess(ignored ->
                                log.info("Finished indexing document {} with id {}", source.title(), source.documentId())
                        ));
    }


    @Override
    public boolean supports(DocumentIndexerType indexerType) {
        return DocumentIndexerType.CHUNKER.equals(indexerType);
    }
}
