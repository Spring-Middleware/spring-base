package io.github.spring.middleware.ai.ollama.config;

import io.github.spring.middleware.ai.provider.config.AbstractHttpAIProviderProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "middleware.ai.provider.ollama")
public class OllamaAIProperties extends AbstractHttpAIProviderProperties {

    public OllamaAIProperties() {
        setProvider("ollama");
    }


}
