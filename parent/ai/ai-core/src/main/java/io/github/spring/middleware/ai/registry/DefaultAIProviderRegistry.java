package io.github.spring.middleware.ai.registry;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.provider.AIProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.StringTemplate.STR;

@Component
public class DefaultAIProviderRegistry implements AIProviderRegistry {

    private final List<AIProvider> providers;

    public DefaultAIProviderRegistry(List<AIProvider> providers) {
        this.providers = providers;
    }

    @Override
    public AIProvider resolve(String model) {
        if (model == null || model.isBlank()) {
            throw new AIException(AIErrorCodes.UNSUPPORTED_AI_MODEL, "AI model must not be null or blank");
        }

        return providers.stream()
                .filter(provider -> provider.supports(model))
                .findFirst()
                .orElseThrow(() -> new AIException(
                        AIErrorCodes.AI_PROVIDER_NOT_FOUND,
                        STR."No AI provider found for model: \{model}"
                ));
    }
}
