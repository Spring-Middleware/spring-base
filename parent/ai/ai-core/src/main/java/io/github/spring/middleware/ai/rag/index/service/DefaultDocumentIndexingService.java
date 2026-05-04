package io.github.spring.middleware.ai.rag.index.service;

import io.github.spring.middleware.ai.rag.index.DocumentClassifierParameters;
import io.github.spring.middleware.ai.rag.index.DocumentIndexer;
import io.github.spring.middleware.ai.rag.index.DocumentIndexerOptions;
import io.github.spring.middleware.ai.rag.index.DocumentSourceIndexClassifier;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import io.github.spring.middleware.ai.rag.source.DocumentSourceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDocumentIndexingService implements DocumentIndexingService {

    private final DocumentSourceRegistry documentSourceRegistry;
    private final DocumentSourceIndexClassifier documentSourceIndexClassifier;
    private final List<DocumentIndexer<?>> documentIndexers;

    @Override
    public Mono<Void> indexSource(
            String sourceName,
            DocumentClassifierParameters<?> parameters
    ) {
        return documentSourceRegistry.resolve(sourceName)
                .flatMap(documentSource ->
                        indexDocumentSource(sourceName, documentSource, parameters)
                )
                .then(Mono.fromRunnable(() ->
                        log.info(STR."Completed indexing documents from sourceName: \{sourceName}")
                ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends DocumentIndexerOptions> Mono<Void> indexDocumentSource(
            String sourceName,
            DocumentSource documentSource,
            DocumentClassifierParameters<?> parameters
    ) {
        I options = documentSourceIndexClassifier.classify(
                sourceName,
                documentSource,
                parameters
        );

        DocumentIndexer<I> indexer = (DocumentIndexer<I>) documentIndexers.stream()
                .filter(candidate -> candidate.supports(options.getIndexerType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        STR."No DocumentIndexer found for type: \{options.getIndexerType()}"
                ));

        return indexer.index(sourceName, documentSource, options);
    }
}
