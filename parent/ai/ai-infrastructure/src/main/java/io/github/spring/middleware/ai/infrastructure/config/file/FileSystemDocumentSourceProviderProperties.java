package io.github.spring.middleware.ai.infrastructure.config.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("middleware.ai.document.source.file-system")
public class FileSystemDocumentSourceProviderProperties {

    private List<String> paths = List.of("docs", "README.md");
}
