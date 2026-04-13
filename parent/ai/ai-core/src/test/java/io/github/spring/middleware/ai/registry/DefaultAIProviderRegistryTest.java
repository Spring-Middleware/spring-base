package io.github.spring.middleware.ai.registry;

import io.github.spring.middleware.ai.exception.AIErrorCodes;
import io.github.spring.middleware.ai.exception.AIException;
import io.github.spring.middleware.ai.provider.AIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAIProviderRegistryTest {

    private DefaultAIProviderRegistry registry;
    private AIProvider provider1;
    private AIProvider provider2;

    @BeforeEach
    void setUp() {
        provider1 = Mockito.mock(AIProvider.class);
        provider2 = Mockito.mock(AIProvider.class);
        registry = new DefaultAIProviderRegistry(List.of(provider1, provider2));
    }

    @Test
    void shouldResolveProviderIfSupportsModel() {
        Mockito.when(provider1.supports("model-1")).thenReturn(false);
        Mockito.when(provider2.supports("model-1")).thenReturn(true);

        AIProvider resolved = registry.resolve("model-1");

        assertNotNull(resolved);
        assertEquals(provider2, resolved);
    }

    @Test
    void shouldThrowExceptionWhenModelIsNull() {
        AIException exception = assertThrows(AIException.class, () -> registry.resolve(null));
        assertEquals(AIErrorCodes.UNSUPPORTED_AI_MODEL, exception.getCode());
        assertEquals("AI model must not be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenModelIsBlank() {
        AIException exception = assertThrows(AIException.class, () -> registry.resolve("   "));
        assertEquals(AIErrorCodes.UNSUPPORTED_AI_MODEL, exception.getCode());
        assertEquals("AI model must not be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNoProviderFound() {
        Mockito.when(provider1.supports("unknown-model")).thenReturn(false);
        Mockito.when(provider2.supports("unknown-model")).thenReturn(false);

        AIException exception = assertThrows(AIException.class, () -> registry.resolve("unknown-model"));
        assertEquals(AIErrorCodes.AI_PROVIDER_NOT_FOUND, exception.getCode());
        assertTrue(exception.getMessage().contains("No AI provider found for model: unknown-model"));
    }
}

