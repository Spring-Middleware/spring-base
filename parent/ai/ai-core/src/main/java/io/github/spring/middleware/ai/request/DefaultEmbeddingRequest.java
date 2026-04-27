package io.github.spring.middleware.ai.request;

public class DefaultEmbeddingRequest implements EmbeddingRequest {

    private final String model;
    private final String input;

    public DefaultEmbeddingRequest(String model, String input) {
        this.model = model;
        this.input = input;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getInput() {
        return this.input;
    }
}
