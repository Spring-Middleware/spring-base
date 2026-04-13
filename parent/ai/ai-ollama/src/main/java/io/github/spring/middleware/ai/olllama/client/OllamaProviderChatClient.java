package io.github.spring.middleware.ai.olllama.client;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.message.AIMessage;
import io.github.spring.middleware.ai.message.AIRole;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.olllama.config.OllamaAIProperties;
import io.github.spring.middleware.ai.olllama.message.OllamaMessage;
import io.github.spring.middleware.ai.olllama.request.OllamaChatRequest;
import io.github.spring.middleware.ai.olllama.response.OllamaChatResponse;
import io.github.spring.middleware.ai.provider.ProviderChatClient;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import io.github.spring.middleware.ai.response.DefaultChatResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OllamaProviderChatClient implements ProviderChatClient {

    private final WebClient webClient;

    public OllamaProviderChatClient(OllamaAIProperties properties, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        if (request == null) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "Chat request must not be null"
            );
        }

        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_MODEL,
                    "Chat request model must not be null or blank"
            );
        }

        OllamaChatRequest ollamaRequest = toOllamaChatRequest(request);

        OllamaChatResponse response = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .block();

        if (response == null) {
            throw new AIException(
                    AIErrorCodes.AI_RESPONSE_ERROR,
                    "Ollama returned a null response"
            );
        }

        if (response.message() == null) {
            throw new AIException(
                    AIErrorCodes.AI_RESPONSE_MAPPING_ERROR,
                    "Ollama response message must not be null"
            );
        }

        return new DefaultChatResponse(toAIMessage(response.message()));
    }

    private OllamaChatRequest toOllamaChatRequest(ChatRequest request) {
        return new OllamaChatRequest(
                request.getModel(),
                request.getMessages().stream()
                        .map(this::toOllamaMessage)
                        .toList(),
                false
        );
    }

    private OllamaMessage toOllamaMessage(AIMessage message) {
        if (message == null) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "AI message must not be null"
            );
        }

        return new OllamaMessage(
                mapRole(message.role()),
                message.content()
        );
    }

    private AIMessage toAIMessage(OllamaMessage message) {
        return new DefaultAIMessage(
                mapRole(message.role()),
                message.content()
        );
    }

    private String mapRole(AIRole role) {
        if (role == null) {
            throw new AIException(
                    AIErrorCodes.INVALID_AI_REQUEST,
                    "AI message role must not be null"
            );
        }

        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }

    private AIRole mapRole(String role) {
        return switch (role) {
            case "system" -> AIRole.SYSTEM;
            case "user" -> AIRole.USER;
            case "assistant" -> AIRole.ASSISTANT;
            default -> throw new AIException(
                    AIErrorCodes.AI_RESPONSE_MAPPING_ERROR,
                    STR."Unsupported role: \{role}"
            );
        };
    }
}
