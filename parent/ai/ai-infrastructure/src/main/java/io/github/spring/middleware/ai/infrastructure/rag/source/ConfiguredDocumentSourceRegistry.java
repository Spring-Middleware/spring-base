package io.github.spring.middleware.ai.infrastructure.rag.source;

import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceDefinition;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceProperties;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceRegistry;
import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConfiguredDocumentSourceRegistry implements DocumentSourceRegistry {

    private final DocumentSourceProperties properties;
    private final List<DocumentSourceProvider<?>> providers;

    public Flux<DocumentSource> resolve(String sourceName) {
        DocumentSourceDefinition definition = Optional
                .ofNullable(properties.getSources().get(sourceName))
                .orElseThrow(() -> new IllegalArgumentException(
                        STR."Unknown document source: \{sourceName}"
                ));

        DocumentSourceProvider provider = providers.stream()
                .filter(p -> p.type() == definition.getType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        STR."No provider found for source type: \{definition.getType()}"
                ));

        DocumentSourceProviderOptions documentSourceProviderOptions = definition.optionsForType();

        return provider.load(sourceName, documentSourceProviderOptions);
    }
}
