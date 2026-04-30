package io.github.spring.middleware.ai.rag.chunk.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.github.spring.middleware.ai.rag.chunk.ChunkerSuitability;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunkInput;
import io.github.spring.middleware.ai.rag.chunk.DocumentChunker;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.github.spring.middleware.ai.rag.chunk.json.JsonChunkerHelper.buildText;

@Component
@RequiredArgsConstructor
public class JsonChunker implements DocumentChunker<JsonChunkerOptions> {

    private final JsonChunkRulesLoader rulesLoader;

    private final Configuration jsonPathConfiguration = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    @Override
    public Flux<DocumentChunkInput> chunk(DocumentSource source, JsonChunkerOptions chunkOptions) {
        return Flux.defer(() -> {
            JsonChunkRulesDefinition rulesDefinition = rulesLoader.load(chunkOptions.rulesPath());

            String json = readSourceAsString(source);
            DocumentContext document = JsonPath.using(jsonPathConfiguration).parse(json);

            return Flux.fromIterable(rulesDefinition.rules())
                    .filter(rule -> rule.extractorPath() != null && !rule.extractorPath().isBlank())
                    .flatMap(rule -> applyRule(document, rule));
        });
    }


    private Flux<DocumentChunkInput> applyRule(
            DocumentContext document,
            JsonChunkRule rule
    ) {
        List<Object> selectedNodes = document.read(rule.extractorPath());

        return Flux.fromIterable(selectedNodes).flatMap(node -> {
            List<DocumentChunkInput> chunks = new ArrayList<>();
            generateJsonChunkExtractorRuleResult(
                    node,
                    rule,
                    new ArrayList<>(),
                    chunks
            );
            return Flux.fromIterable(chunks);
        });
    }

    private void generateJsonChunkExtractorRuleResult(
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

        List<JsonChunkExtractorRuleResult> filteredInherited = inheritOnlyFields(inheritedResults);

        List<JsonChunkExtractorRuleResult> currentResults = new ArrayList<>(filteredInherited);
        currentResults.addAll(ownResults);

        if (!rule.generationTextRules().isEmpty()) {
            String text = buildText(currentResults, rule.generationTextRules());
            Map<String, Object> metadata = buildMetaData(currentResults);

            chunks.add(new DocumentChunkInput(text, metadata)); // ajusta constructor
        }

        for (JsonChunkRule childRule : rule.children()) {
            List<Object> childrenNodes = nodeContext.read(childRule.extractorPath());

            for (Object childNode : childrenNodes) {
                generateJsonChunkExtractorRuleResult(
                        childNode,
                        childRule,
                        currentResults,
                        chunks
                );
            }
        }
    }

    private static List<JsonChunkExtractorRuleResult> inheritOnlyFields(
            List<JsonChunkExtractorRuleResult> results
    ) {
        return results.stream()
                .filter(result -> result.jsonDataTypes().contains(JsonDataType.FIELD))
                .map(result -> new JsonChunkExtractorRuleResult(
                        List.of(JsonDataType.FIELD),
                        result.name(),
                        result.result()
                ))
                .toList();
    }

    private Map<String, Object> buildMetaData(List<JsonChunkExtractorRuleResult> ruleResults) {
        return ruleResults.stream()
                .filter(result -> result.jsonDataTypes().contains(JsonDataType.META_DATA))
                .filter(result -> result.result() != null)
                .collect(Collectors.toMap(
                        JsonChunkExtractorRuleResult::name,
                        JsonChunkExtractorRuleResult::result,
                        (left, right) -> left
                ));
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


}
