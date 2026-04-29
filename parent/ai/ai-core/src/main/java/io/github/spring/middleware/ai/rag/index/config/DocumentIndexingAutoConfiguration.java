package io.github.spring.middleware.ai.rag.index.config;

import io.github.spring.middleware.ai.rag.index.DocumentClassifierParameters;
import io.github.spring.middleware.ai.rag.index.DocumentIndexer;
import io.github.spring.middleware.ai.rag.index.service.DocumentIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

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
    public ApplicationRunner documentIndexingApplicationRunner(
            DocumentIndexingService documentIndexingService,
            DocumentIndexingProperties indexingProperties
    ) {
        return args -> Flux.fromIterable(indexingProperties.getSources().keySet())
                .flatMap(sourceName -> {
                    DocumentIndexingProperties.DocumentIndexingSourceProperties sourceIndexing =
                            indexingProperties.getSources().get(sourceName);

                    DocumentClassifierParameters<?> parameters =
                            DocumentClassifierParameters.from(sourceName, sourceIndexing);

                    return documentIndexingService.indexSource(sourceName, parameters);
                })
                .then()
                .block();
    }

}
