package io.github.spring.middleware.ai.chat.controller;

import io.github.spring.middleware.ai.chat.domain.DocumentationConversationResponse;
import io.github.spring.middleware.ai.chat.request.DocumentationChatRequest;
import io.github.spring.middleware.ai.chat.service.DocumentationChatService;
import io.github.spring.middleware.ai.response.ChatResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class DocumentationChatController {

    private final DocumentationChatService documentationChatService;

    public DocumentationChatController(DocumentationChatService documentationChatService) {
        this.documentationChatService = documentationChatService;
    }

    @PostMapping("/start")
    public Mono<DocumentationConversationResponse> startConversation(
            @RequestBody DocumentationChatRequest request) {

        return documentationChatService.startConversation(
                request.sourceName(),
                request.model(),
                request.question()
        );
    }

    @PostMapping("/{conversationId}/ask")
    public Mono<ChatResponse> ask(
            @PathVariable("conversationId") UUID conversationId,
            @RequestBody DocumentationChatRequest request) {

        return documentationChatService
                .ask(conversationId, request.sourceName(), request.model(), request.question());
    }

    @PostMapping("/ask")
    public Mono<ChatResponse> ask(@RequestBody DocumentationChatRequest request) {

        return documentationChatService
                .ask(request.sourceName(), request.model(), request.question());
    }

}