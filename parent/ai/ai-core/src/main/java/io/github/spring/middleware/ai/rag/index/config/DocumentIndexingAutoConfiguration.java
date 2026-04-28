package io.github.spring.middleware.ai.rag.index.config;

import io.github.spring.middleware.ai.rag.index.DocumentIndexer;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerType;
import io.github.spring.middleware.ai.rag.index.DocumentSourceIndexClassifier;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProvider;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProviderRegistration;
import io.github.spring.middleware.ai.rag.source.config.DocumentSourceProviderOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DocumentIndexingProperties.class)
@ConditionalOnClass(DocumentIndexer.class)
@ConditionalOnProperty(
        prefix = "middleware.ai.document.indexing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DocumentIndexingAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "middleware.ai.document.indexing",
            name = "index-on-startup",
            havingValue = "true"
    )
    @SuppressWarnings("unchecked")
    public <O extends DocumentSourceProviderOptions, I extends DocumentIndexerOptions> ApplicationRunner documentIndexingApplicationRunner(
            DocumentSourceIndexClassifier documentSourceIndexClassifier,
            List<DocumentIndexer<?>> documentIndexers,
            List<DocumentSourceProviderRegistration<?, ?>> registrations
    ) {
        return args -> Flux.fromIterable(registrations)
                .flatMap(registration ->
                        loadSources(registration)
                                .flatMap(documentSource ->
                                        indexDocument(
                                                documentSource,
                                                documentSourceIndexClassifier,
                                                documentIndexers
                                        )
                                )
                                .then(Mono.fromRunnable(() ->
                                        log.info(STR."Completed indexing documents from provider: \{registration.sourceProvider().getClass().getSimpleName()}")
                                ))
                )
                .then(Mono.fromRunnable(() ->
                        log.info("Document indexing completed on startup")
                ))
                .block();
    }

    @SuppressWarnings("unchecked")
    private <I extends DocumentIndexerOptions> Mono<Void> indexDocument(
            DocumentSource documentSource,
            DocumentSourceIndexClassifier classifier,
            List<DocumentIndexer<?>> documentIndexers
    ) {
        I options = classifier.classify(documentSource);

        DocumentIndexer<I> indexer = (DocumentIndexer<I>) getDocumentIndexer(
                documentIndexers,
                options.getIndexerType()
        );

        return indexer.index(documentSource, options);
    }


    @SuppressWarnings("unchecked")
    private <O extends DocumentSourceProviderOptions> Flux<DocumentSource> loadSources(
            DocumentSourceProviderRegistration<O, ? extends DocumentSourceProvider<O>> registration
    ) {
        return registration.sourceProvider().load(registration.options());
    }


    private DocumentIndexer getDocumentIndexer(List<DocumentIndexer<?>> documentIndexers, DocumentIndexerType documentIndexerType) {
        return documentIndexers.stream().filter(indexer -> indexer.supports(documentIndexerType)).findFirst().orElseThrow(() -> new IllegalStateException(STR."No DocumentIndexer found for type: \{documentIndexerType}"));
    }

}
