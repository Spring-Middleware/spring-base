package io.github.spring.middleware.ai.source;

import io.github.spring.middleware.ai.infrastructure.rag.source.custom.AbstarctCustomDocumentSourceProvider;
import io.github.spring.middleware.ai.infrastructure.rag.source.custom.CustomDocumentSourceProviderOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.SourceProviderName;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@SourceProviderName("catalogs-provider")
public class CustomDocumentSource extends AbstarctCustomDocumentSourceProvider {

    @Override
    protected Flux<DocumentSource> loadSource(String sourceName, CustomDocumentSourceProviderOptions options) {
        return Flux.defer(() -> {
            try {
                ClassPathResource resource = new ClassPathResource("json/catalogs.json");
                DocumentSource documentSource = new DocumentSource(
                        "catalogs",
                        "catalogs.json",
                        resource.getInputStream(),
                        "json",
                        "application/json",
                        Map.of(),
                        Instant.ofEpochMilli(resource.lastModified())
                );
                return Flux.just(documentSource);
            } catch (IOException e) {
                return Flux.error(e);
            }
        });
    }

}
