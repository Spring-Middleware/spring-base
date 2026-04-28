package io.github.spring.middleware.ai.infrastructure.rag.source.mongo;

import io.github.spring.middleware.ai.infrastructure.config.mongo.MongoDocumentSourceProviderProperties.DocumentCollection;
import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;

import java.util.List;

public record MongoDocumentSourceProviderOptions(
        List<DocumentCollection> collections
) implements DocumentSourceProviderOptions {
}
