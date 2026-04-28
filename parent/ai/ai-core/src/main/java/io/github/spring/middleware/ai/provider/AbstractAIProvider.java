package io.github.spring.middleware.ai.provider;

import io.github.spring.middleware.ai.provider.config.AbstractHttpAIProviderProperties;

import java.time.Duration;

public abstract class AbstractAIProvider<P extends AbstractHttpAIProviderProperties> implements AIProvider {

    protected final P properties;

    protected AbstractAIProvider(P properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String model) {
        if (!properties.isEnabled()) {
            return false;
        }

        if (model == null || model.isBlank()) {
            return false;
        }

        return properties.getModels()
                .stream()
                .anyMatch(m -> m.equalsIgnoreCase(model));
    }

    protected String getDefaultModel() {
        return properties.getDefaultModel();
    }

    protected String getBaseUrl() {
        return properties.getBaseUrl();
    }

    protected Duration getConnectTimeout() {
        return properties.getConnectTimeout();
    }

    protected Duration getReadTimeout() {
        return properties.getReadTimeout();
    }

}
