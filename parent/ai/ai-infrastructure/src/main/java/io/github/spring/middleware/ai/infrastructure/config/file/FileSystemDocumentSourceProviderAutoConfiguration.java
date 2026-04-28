package io.github.spring.middleware.ai.infrastructure.config.file;

import io.github.spring.middleware.ai.infrastructure.rag.source.file.FileSystemDocumentSourceProvider;
import io.github.spring.middleware.ai.infrastructure.rag.source.file.FileSystemDocumentSourceProviderOptions;
import io.github.spring.middleware.ai.rag.source.DocumentSourceProviderRegistration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FileSystemDocumentSourceProviderProperties.class)
public class FileSystemDocumentSourceProviderAutoConfiguration {

    @Bean
    public DocumentSourceProviderRegistration<FileSystemDocumentSourceProviderOptions, FileSystemDocumentSourceProvider> fileSystemDocumentSourceProviderRegistration(FileSystemDocumentSourceProvider sourceProvider, FileSystemDocumentSourceProviderProperties properties) {
        final var options = new FileSystemDocumentSourceProviderOptions(properties.getPaths());
        return new DocumentSourceProviderRegistration<>(options, sourceProvider);
    }
}
