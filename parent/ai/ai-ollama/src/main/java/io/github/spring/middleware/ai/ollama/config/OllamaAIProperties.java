package io.github.spring.middleware.ai.ollama.config;

import io.github.spring.middleware.ai.config.AbstractHttpAIProviderProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "middleware.ai.provider.ollama")
public class OllamaAIProperties extends AbstractHttpAIProviderProperties {


}
