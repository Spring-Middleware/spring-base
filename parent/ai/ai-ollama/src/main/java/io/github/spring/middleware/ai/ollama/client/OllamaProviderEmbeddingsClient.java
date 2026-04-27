package io.github.spring.middleware.ai.ollama.client;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.ollama.config.OllamaAIProperties;
import io.github.spring.middleware.ai.provider.ProviderEmbeddingClient;
import io.github.spring.middleware.ai.request.EmbeddingRequest;
import io.github.spring.middleware.ai.response.DefaultEmbeddingResponse;
import io.github.spring.middleware.ai.response.EmbeddingResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class OllamaProviderEmbeddingsClient implements ProviderEmbeddingClient {

    private final WebClient webClient;

    public OllamaProviderEmbeddingsClient(OllamaAIProperties properties,
                                          WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public EmbeddingResponse generate(EmbeddingRequest request) {

        if (request == null) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "Embedding request must not be null"
            );
        }

        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_MODEL,
                    "Embedding model must not be null or blank"
            );
        }

        if (request.getInput() == null || request.getInput().isBlank()) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "Embedding input must not be null or blank"
            );
        }

        // Request hacia Ollama
        Map<String, Object> body = Map.of(
                "model", request.getModel(),
                "prompt", request.getInput()
        );

        OllamaEmbeddingResponse response = webClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OllamaEmbeddingResponse.class)
                .block();

        if (response == null || response.embedding() == null) {
            throw new AIException(
                    AIErrorCodes.AI_RESPONSE_ERROR,
                    "Ollama embedding response is null"
            );
        }

        return new DefaultEmbeddingResponse(response.embedding());
    }

    // --- DTO interno ---
    public record OllamaEmbeddingResponse(List<Float> embedding) {
    }
}
