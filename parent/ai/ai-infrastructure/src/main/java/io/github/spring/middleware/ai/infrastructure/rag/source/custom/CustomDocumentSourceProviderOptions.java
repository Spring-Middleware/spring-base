package io.github.spring.middleware.ai.infrastructure.rag.source.custom;

import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;

import java.util.Map;

public record CustomDocumentSourceProviderOptions(
        Map<String, Object> properties) implements DocumentSourceProviderOptions {

}
