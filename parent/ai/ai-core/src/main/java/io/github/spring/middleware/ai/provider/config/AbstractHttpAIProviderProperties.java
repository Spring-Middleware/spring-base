package io.github.spring.middleware.ai.provider.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHttpAIProviderProperties {

    /**
     * Enables or disables the provider.
     */
    private boolean enabled = true;

    /**
     * Logical provider name (ollama, openai, etc.)
     */
    private String provider;

    /**
     * Supported models by this provider.
     */
    private List<String> models = new ArrayList<>();

    /**
     * Default model for this provider (optional).
     */
    private String defaultModel;

    /**
     * Base URL of the provider API.
     */
    private String baseUrl;

    /**
     * Connection timeout in milliseconds.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Read timeout in milliseconds.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    // --- getters & setters ---

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
