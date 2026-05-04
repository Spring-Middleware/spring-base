package io.github.spring.middleware.ai.infrastructure.rag.source;

import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceDefinition;
import io.github.spring.middleware.ai.infrastructure.rag.source.config.DocumentSourceProperties;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceRegistry;
import io.github.spring.middleware.ai.rag.source.DocumentSourceType;
import io.github.spring.middleware.ai.rag.source.SourceProviderName;
import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Slf4j
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

        if (definition.isEnabled()) {
            DocumentSourceProvider provider = resolveProvider(definition);
            DocumentSourceProviderOptions options = definition.optionsForType();
            return provider.load(sourceName, options);
        }else{
            log.warn("Document source with name: {} is disabled and will not be loaded", sourceName);
            return Flux.empty();
        }
    }

    private DocumentSourceProvider<?> resolveProvider(DocumentSourceDefinition definition) {
        if (definition.getType() == DocumentSourceType.CUSTOM) {
            String providerName = definition.getProviderName();

            if (providerName == null || providerName.isBlank()) {
                throw new IllegalArgumentException(
                        "Custom document source requires provider-name"
                );
            }

            return providers.stream()
                    .filter(p -> p.type() == DocumentSourceType.CUSTOM)
                    .filter(p -> {
                        SourceProviderName annotation =
                                p.getClass().getAnnotation(SourceProviderName.class);

                        return annotation != null && annotation.value().equals(providerName);
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            STR."No custom DocumentSourceProvider found with name: \{providerName}"
                    ));
        }

        return providers.stream()
                .filter(p -> p.type() == definition.getType())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        STR."No provider found for source type: \{definition.getType()}"
                ));
    }

}
