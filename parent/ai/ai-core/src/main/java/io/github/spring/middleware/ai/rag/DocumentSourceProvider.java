package io.github.spring.middleware.ai.rag;

import reactor.core.publisher.Flux;

public interface DocumentSourceProvider<P extends DocumentSourceProviderProperties> {

    Flux<DocumentSource> load(P properties);

}
