package io.github.spring.middleware.ai.rag.source;

import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;


public record DocumentSourceProviderRegistration<O extends DocumentSourceProviderOptions, S extends DocumentSourceProvider<O>>
        (O options, S sourceProvider) {
}
