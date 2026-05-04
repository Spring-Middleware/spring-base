package io.github.spring.middleware.ai.rag.context;

import reactor.core.publisher.Mono;

public interface RagContextBuilder {

    Mono<RagContext> build(RagContextRequest request);

}
