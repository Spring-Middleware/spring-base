package io.github.spring.middleware.ai.conversation.client;

import io.github.spring.middleware.ai.client.ChatClient;
import io.github.spring.middleware.ai.conversation.Conversation;
import io.github.spring.middleware.ai.request.ChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultConversationClient implements ConversationClient {

    private final ChatClient chatClient;

    public DefaultConversationClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ChatResponse chat(Conversation conversation, String model, String userMessage, String context) {
        if (conversation == null) {
            throw new IllegalArgumentException("conversation must not be null");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be null or blank");
        }
        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage must not be null");
        }

        String augmentedQuestion = """
                Documentation context:
                
                %s
                
                User question:
                
                %s
                """.formatted(context, userMessage);

        Conversation requestConversation = conversation.copy(); // o clone/snapshot
        requestConversation.addUserMessage(augmentedQuestion);

        ChatRequest chatRequest = requestConversation.toRequest(model);

        ChatResponse response = chatClient.generate(chatRequest);

        conversation.addUserMessage(userMessage);
        if (response != null && response.getMessage() != null) {
            conversation.addMessage(response.getMessage());
        }

        return response;
    }
}
