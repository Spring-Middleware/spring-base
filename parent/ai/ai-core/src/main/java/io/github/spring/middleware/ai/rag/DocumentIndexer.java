package io.github.spring.middleware.ai.rag;

import reactor.core.publisher.Mono;

public interface DocumentIndexer {

    Mono<Void> index(DocumentSource source, String embeddingModel);

}
