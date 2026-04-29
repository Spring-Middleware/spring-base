package io.github.spring.middleware.ai.rag.source;

import reactor.core.publisher.Flux;

public interface DocumentSourceRegistry {

    Flux<DocumentSource> resolve(String sourceName);

}
