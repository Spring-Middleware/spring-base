package io.github.spring.middleware.ai.infrastructure.config.custom;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties("middleware.ai.document.source.custom")
public class CustomDocumentSourceProviderProperties {

    private Map<String, Object> properties;

}
