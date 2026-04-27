package io.github.spring.middleware.ai.provider;

public interface AIProvider {

    boolean supports(String model);

    boolean supportsChat();

    boolean supportsEmbeddings();

    ProviderChatClient getChatClient();

    ProviderEmbeddingClient getEmbeddingClient();

    boolean isAvailable();

}
