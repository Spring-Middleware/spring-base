package io.github.spring.middleware.ai.rag.source;

import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;
import reactor.core.publisher.Flux;

public interface DocumentSourceProvider<O extends DocumentSourceProviderOptions> {

    Flux<DocumentSource> load(O options);

}
