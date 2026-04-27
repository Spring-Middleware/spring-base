package io.github.spring.middleware.ai.ollama.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.spring.middleware.ai.ollama.message.OllamaMessage;

public record OllamaChatResponse(

        String model,

        @JsonProperty("created_at")
        String createdAt,

        OllamaMessage message,

        boolean done,

        @JsonProperty("total_duration")
        Long totalDuration,

        @JsonProperty("load_duration")
        Long loadDuration,

        @JsonProperty("prompt_eval_count")
        Integer promptEvalCount,

        @JsonProperty("prompt_eval_duration")
        Long promptEvalDuration,

        @JsonProperty("eval_count")
        Integer evalCount,

        @JsonProperty("eval_duration")
        Long evalDuration

) {
}
