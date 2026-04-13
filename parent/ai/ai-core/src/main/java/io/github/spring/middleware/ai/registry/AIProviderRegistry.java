package io.github.spring.middleware.ai.registry;

import io.github.spring.middleware.ai.provider.AIProvider;

public interface AIProviderRegistry {

    AIProvider resolve(String model);

}
