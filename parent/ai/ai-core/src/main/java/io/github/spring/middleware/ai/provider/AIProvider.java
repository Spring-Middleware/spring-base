package io.github.spring.middleware.ai.provider;

public interface AIProvider {

    boolean supports(String model);

    ProviderChatClient getChatClient();

}
