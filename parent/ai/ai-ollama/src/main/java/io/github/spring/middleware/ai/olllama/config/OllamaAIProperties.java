package io.github.spring.middleware.ai.olllama.config;

import io.github.spring.middleware.ai.config.AbstractHttpAIProviderProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "middleware.ai.provider.olllama")
public class OllamaAIProperties extends AbstractHttpAIProviderProperties {


}
