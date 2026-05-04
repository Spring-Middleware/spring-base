package io.github.spring.middleware.ai.rag.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.ai.client.ChatClient;
import io.github.spring.middleware.ai.message.DefaultAIMessage;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.registry.DocumentChunkerRegistry;
import io.github.spring.middleware.ai.rag.vector.VectorStore;
import io.github.spring.middleware.ai.request.DefaultChatRequest;
import io.github.spring.middleware.ai.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientRagQueryPlanner implements RagQueryPlanner {

    private final ChatClient chatClient;
    private final DocumentChunkerRegistry documentChunkerRegistry;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public Mono<RagQueryPlan> plan(SelfQueryRequest request) {
        DocumentChunker<?> documentChunker = documentChunkerRegistry.findByName(request.chunker());
        List<String> metadaFields = documentChunker.getMetadataFields(request.sourceName()); // Warm up any caches to ensure fast response

        if (metadaFields == null || metadaFields.isEmpty()) {
            return Mono.just(fallbackPlan(request));
        }

        return doPlan(request, metadaFields)
                .onErrorResume(error -> {
                    log.warn("Failed to extract RAG query plan. Falling back to semantic search.", error);
                    return Mono.just(fallbackPlan(request));
                });
    }

    private Mono<RagQueryPlan> doPlan(SelfQueryRequest request, List<String> metadataFields) {
        String availableFields = String.join("\n- ", metadataFields);

        String template = getPlannerTemplate(request.sourceName());

        String systemPrompt = String.format(
                template,
                STR."- \{availableFields}"
        );

        DefaultChatRequest chatRequest = new DefaultChatRequest(request.model());
        chatRequest.addMessage(DefaultAIMessage.system(systemPrompt));
        chatRequest.addMessage(DefaultAIMessage.user(request.query()));

        return chatClient.generate(chatRequest).map(response -> {
            try {
                String rawContent = response.getMessage().content();

                String json = extractJson(rawContent);
                RagQueryPlan rawPlan = objectMapper.readValue(json, RagQueryPlan.class);

                return sanitizePlan(request, rawPlan, metadataFields);
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract RAG query plan from chat response", e);
            }
        });
    }

    private RagQueryPlan sanitizePlan(SelfQueryRequest request, RagQueryPlan plan, List<String> metadataFields) {
        Set<String> allowedFields = metadataFields
                .stream()
                .collect(Collectors.toSet());

        List<MetadataFilter> safeFilters = plan.filters() == null
                ? List.of()
                : plan.filters().stream()
                .filter(filter -> filter != null)
                .filter(filter -> filter.field() != null && allowedFields.contains(filter.field()))
                .filter(filter -> filter.values() != null && !filter.values().isEmpty())
                .map(filter -> new MetadataFilter(
                        filter.field(),
                        filter.values().stream()
                                .filter(value -> value != null && !value.isBlank())
                                .map(String::trim)
                                .distinct()
                                .toList(),
                        filter.matchType() == null
                                ? VectorStore.MatchType.MATCH_ANY
                                : filter.matchType()
                ))
                .filter(filter -> !filter.values().isEmpty())
                .toList();

        String optimizedQuery = plan.optimizedQuery() == null || plan.optimizedQuery().isBlank()
                ? request.query()
                : plan.optimizedQuery().trim();

        boolean useSemanticSearch = plan.useSemanticSearch() || safeFilters.isEmpty();

        return new RagQueryPlan(
                optimizedQuery,
                safeFilters,
                useSemanticSearch
        );
    }

    private RagQueryPlan fallbackPlan(SelfQueryRequest request) {
        return new RagQueryPlan(
                request.query(),
                List.of(),
                true
        );
    }

    private String getPlannerTemplate(String sourceName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:planner/" + sourceName + ".planner");
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Could not load planner template for source: {}", sourceName, e);
        }
        return PromptPlanner.SYSTEM_PROMPT_TEMPLATE;
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Empty query planner response");
        }

        String trimmed = content.trim();

        if (trimmed.contains("```json")) {
            return trimmed.substring(
                    trimmed.indexOf("```json") + 7,
                    trimmed.lastIndexOf("```")
            ).trim();
        }

        if (trimmed.contains("```")) {
            return trimmed.substring(
                    trimmed.indexOf("```") + 3,
                    trimmed.lastIndexOf("```")
            ).trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        throw new IllegalArgumentException("No JSON object found in query planner response");
    }
}
