package io.github.spring.middleware.ai.infrastructure.config.mongo;

import io.github.spring.middleware.ai.infrastructure.rag.source.mongo.MongoDocumentSourceProvider;
import io.github.spring.middleware.ai.infrastructure.rag.source.mongo.MongoDocumentSourceProviderOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProviderRegistration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MongoDocumentSourceProviderProperties.class)
public class MongoDocumentSourceProviderAutoConfiguration {

    @Bean
    public DocumentSourceProviderRegistration<MongoDocumentSourceProviderOptions, MongoDocumentSourceProvider> mongoDocumentSourceProviderRegistration(MongoDocumentSourceProvider sourceProvider, MongoDocumentSourceProviderProperties properties) {
        final var options = new MongoDocumentSourceProviderOptions(properties.getCollections());
        return new DocumentSourceProviderRegistration<>(options, sourceProvider);
    }
}
