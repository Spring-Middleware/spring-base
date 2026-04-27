package io.github.spring.middleware.ai.infrastructure.config;

import io.github.spring.middleware.ai.rag.DocumentSourceProviderProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
@ConfigurationProperties("middleware.ai.document.source.file-system")
public class FileSystemDocumentSourceProviderProperties implements DocumentSourceProviderProperties {

    private List<String> directories = List.of("docs");

}
