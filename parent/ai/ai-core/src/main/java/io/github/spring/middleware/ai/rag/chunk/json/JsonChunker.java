package io.github.spring.middleware.ai.rag.chunk.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunkInput;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerConfiguration;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.chunk.config.JsonDocumentChunkerProperties;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.spring.middleware.ai.rag.chunk.json.JsonChunkerHelper.buildText;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonChunker implements DocumentChunker<JsonChunkerOptions> {

    private final JsonChunkRulesLoader rulesLoader;
    private final DocumentChunkerConfiguration documentChunkerConfiguration;

    private final Configuration jsonPathConfiguration = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    @Override
    public Flux<DocumentChunkInput> chunk(DocumentSource source, JsonChunkerOptions chunkOptions) {
        return Flux.defer(() -> {
            String json = readSourceAsString(source);
            DocumentContext document = JsonPath.using(jsonPathConfiguration).parse(json);

            return Flux.fromIterable(chunkOptions.rulesPath())
                    .map(rulesLoader::load)
                    .flatMap(rulesDefinition -> {
                        if (rulesDefinition.rules().isEmpty()) {
                            return Flux.error(new IllegalArgumentException(
                                    STR."No rules found in rules definition loaded from paths \{chunkOptions.rulesPath()}"
                            ));
                        }

                        return Flux.fromIterable(rulesDefinition.rules())
                                .filter(rule -> rule.extractorPath() != null && !rule.extractorPath().isBlank())
                                .flatMap(rule -> applyRule(source, document, rule));
                    });
        });
    }


    private Flux<DocumentChunkInput> applyRule(
            DocumentSource source,
            DocumentContext document,
            JsonChunkRule rule
    ) {
        Object selected = document.read(rule.extractorPath());

        return Flux.fromIterable(asIterable(selected))
                .flatMap(node -> {
                    List<DocumentChunkInput> chunks = new ArrayList<>();

                    generateJsonChunkExtractorRuleResult(
                            source,
                            node,
                            rule,
                            new ArrayList<>(),
                            chunks
                    );

                    return Flux.fromIterable(chunks);
                });
    }

    private Iterable<Object> asIterable(Object selected) {
        if (selected == null) {
            return List.of();
        }

        if (selected instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            return values;
        }

        return List.of(selected);
    }

    private void generateJsonChunkExtractorRuleResult(
            DocumentSource source,
            Object node,
            JsonChunkRule rule,
            List<JsonChunkExtractorRuleResult> inheritedResults,
            List<DocumentChunkInput> chunks
    ) {
        DocumentContext nodeContext = JsonPath.using(jsonPathConfiguration).parse(node);

        List<JsonChunkExtractorRuleResult> ownResults = rule.extractorRules()
                .stream()
                .map(extractorRule -> applyJsonExtractorRule(nodeContext, extractorRule))
                .filter(Objects::nonNull)
                .toList();

        List<JsonChunkExtractorRuleResult> filteredInherited = inheritFields(inheritedResults, rule.inheritMetadata());

        List<JsonChunkExtractorRuleResult> currentResults = new ArrayList<>(filteredInherited);
        currentResults.addAll(ownResults);

        if (!rule.generationTextRules().isEmpty()) {
            String text = buildText(currentResults, rule.generationTextRules());
            Map<String, Object> metadata = buildMetaData(source, currentResults);
            log.debug("Generated chunk with text: \n{}\n and metadata: {}", text, metadata);

            chunks.add(new DocumentChunkInput(text, metadata)); // ajusta constructor
        }

        for (JsonChunkRule childRule : rule.children()) {
            List<Object> childrenNodes = nodeContext.read(childRule.extractorPath());

            for (Object childNode : childrenNodes) {
                generateJsonChunkExtractorRuleResult(
                        source,
                        childNode,
                        childRule,
                        currentResults,
                        chunks
                );
            }
        }
    }

    private static List<JsonChunkExtractorRuleResult> inheritFields(
            List<JsonChunkExtractorRuleResult> results,
            List<String> inheritMetadata
    ) {
        Set<String> inheritedMetadataNames = inheritMetadata == null
                ? Set.of()
                : inheritMetadata.stream()
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());

        return results.stream()
                .filter(result -> result.result() != null)
                .filter(result ->
                        result.jsonDataTypes().contains(JsonDataType.FIELD)
                                || inheritedMetadataNames.contains(result.name())
                )
                .map(result -> {
                    if (inheritedMetadataNames.contains(result.name())) {
                        return new JsonChunkExtractorRuleResult(
                                List.of(JsonDataType.FIELD, JsonDataType.META_DATA),
                                result.name(),
                                result.result()
                        );
                    }

                    return new JsonChunkExtractorRuleResult(
                            List.of(JsonDataType.FIELD),
                            result.name(),
                            result.result()
                    );
                })
                .toList();
    }

    private Map<String, Object> buildMetaData(
            DocumentSource source,
            List<JsonChunkExtractorRuleResult> ruleResults
    ) {

        Map<String, Object> metadata = new LinkedHashMap<>();

        // 🔥 1. metadata del source (base común a todos los chunkers)
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }

        // 🔥 2. metadata extraída del JSON (reglas)
        ruleResults.stream()
                .filter(result -> result.jsonDataTypes().contains(JsonDataType.META_DATA))
                .filter(result -> result.result() != null)
                .forEach(result ->
                        metadata.putIfAbsent(result.name(), result.result())
                );

        // 🔥 3. tipo de chunker (clave para debug / planner)
        metadata.put("chunker", "json");

        return metadata;
    }


    private JsonChunkExtractorRuleResult applyJsonExtractorRule(DocumentContext nodeContext, JsonChunkExtractorRule extractorRule) {
        String value = JsonChunkerHelper.readString(nodeContext, extractorRule.extractorPath());
        return new JsonChunkExtractorRuleResult(extractorRule.jsonDataTypes(), extractorRule.name(), value);
    }


    private String readSourceAsString(DocumentSource source) {
        try {
            return new String(source.inputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not read document source", e);
        }
    }


    @Override
    public int suitability(DocumentSource source) {
        boolean jsonExtension =
                "json".equalsIgnoreCase(source.extension())
                        || "json".equalsIgnoreCase(source.extension());

        boolean jsonContentType =
                "application/json".equalsIgnoreCase(source.contentType());

        if (jsonExtension && jsonContentType) {
            return ChunkerSuitability.EXACT_MATCH;
        }

        if (jsonContentType) {
            return ChunkerSuitability.CONTENT_TYPE_MATCH;
        }

        if (jsonExtension) {
            return ChunkerSuitability.EXTENSION_MATCH;
        }

        return ChunkerSuitability.UNSUPPORTED;
    }

    @Override
    public Class<JsonChunkerOptions> optionsType() {
        return JsonChunkerOptions.class;
    }

    @Override
    public List<String> getMetadataFields(String sourceName) {
        Set<String> fields = new LinkedHashSet<>();

        DocumentChunkerProperties properties = documentChunkerConfiguration.getDocumentChunkers().entrySet()
                .stream()
                .filter(entry -> entry.getValue().getType() == DocumentChunkerProperties.DocumentChunkerType.JSON)
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);



        if (properties != null && properties.getJson() != null) {
            JsonDocumentChunkerProperties.JsonDocumentChunkerDefinitionProperties definitionProperties =
                    properties.getJson().getDefinitions().get(sourceName);

            if (definitionProperties != null && definitionProperties.getRulesPaths() != null) {
                definitionProperties.getRulesPaths().stream()
                        .map(rulesLoader::load)
                        .flatMap(definition -> definition.rules().stream())
                        .forEach(rule -> collectMetadataFields(rule, fields));
            }
        }

        fields.add("chunker");

        return List.copyOf(fields);
    }

    private void collectMetadataFields(JsonChunkRule rule, Set<String> fields) {
        if (rule.extractorRules() != null) {
            rule.extractorRules().stream()
                    .filter(extractorRule -> extractorRule.jsonDataTypes() != null)
                    .filter(extractorRule -> extractorRule.jsonDataTypes().contains(JsonDataType.META_DATA))
                    .map(JsonChunkExtractorRule::name)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .forEach(fields::add);
        }

        if (rule.children() != null) {
            rule.children().forEach(child -> collectMetadataFields(child, fields));
        }
    }


}
