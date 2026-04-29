package io.github.spring.middleware.ai.infrastructure.controller;

import io.github.spring.middleware.ai.rag.index.DocumentClassifierParameters;
import io.github.spring.middleware.ai.rag.index.service.DocumentIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DocumentIndexController {

    private final DocumentIndexingService documentIndexingService;

    @PostMapping("/document-index/{sourceName}")
    public Mono<Void> getDocumentIndex(@PathVariable String sourceName, @RequestBody DocumentClassifierParameters parameters) {
        return documentIndexingService.indexSource(sourceName, parameters);
    }
}
