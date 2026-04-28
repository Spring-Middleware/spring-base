package io.github.spring.middleware.ai.infrastructure.rag.source.file;

import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;

import java.util.List;

public record FileSystemDocumentSourceProviderOptions(
        List<String> paths) implements DocumentSourceProviderOptions {
}
