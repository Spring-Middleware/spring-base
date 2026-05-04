package io.github.spring.middleware.ai.rag.chunk.json;

import com.jayway.jsonpath.DocumentContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JsonChunkerHelper {

    private JsonChunkerHelper() {
        // Utility class, prevent instantiation
    }

    public static String readString(DocumentContext context, String path) {
        try {
            Object value = readValue(context, path);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            // Log the error if needed
            return null;
        }
    }

    private static Object readValue(DocumentContext context, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        return context.read(path);
    }


    public static String buildText(
            List<JsonChunkExtractorRuleResult> ruleResults,
            List<JsonChunkGenerationTextRule> generationRules
    ) {
        StringBuilder sb = new StringBuilder();

        Map<String, Object> resultsByName = ruleResults.stream()
                .filter(result -> result.name() != null && !result.name().isBlank())
                .filter(result -> result.result() != null)
                .filter(result -> result.jsonDataTypes().contains(JsonDataType.FIELD))
                .collect(Collectors.toMap(
                        JsonChunkExtractorRuleResult::name,
                        JsonChunkExtractorRuleResult::result,
                        (left, right) -> left
                ));

        for (JsonChunkGenerationTextRule generationRule : generationRules) {
            String rendered = renderTemplate(generationRule, resultsByName);

            if (!rendered.isBlank()) {
                sb.append(rendered).append("\n");
            }
        }

        return sb.toString().trim();
    }


    private static String renderTemplate(
            JsonChunkGenerationTextRule generationRule,
            Map<String, Object> resultsByName
    ) {
        String rendered = generationRule.template();
        boolean hasAnyValue = false;

        for (Map.Entry<String, String> entry : generationRule.variables().entrySet()) {
            String templateVariable = entry.getKey();

            VariableMapping mapping = parseMapping(entry.getValue());

            String value = normalizeValue(resultsByName.get(mapping.field));

            if (value.isBlank() && mapping.fallback != null) {
                value = mapping.fallback;
            }

            if (mapping.required && value.isBlank()) {
                return "";
            }

            if (!value.isBlank()) {
                hasAnyValue = true;
            }

            rendered = rendered.replace(STR."{\{templateVariable}}", value);
        }

        if (!hasAnyValue) {
            return "";
        }

        return rendered
                .replaceAll("\\{[^}]+}", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static VariableMapping parseMapping(String raw) {
        VariableMapping m = new VariableMapping();

        String value = raw;

        // required
        if (value.endsWith("!")) {
            m.required = true;
            value = value.substring(0, value.length() - 1);
        }

        // fallback
        int idx = value.indexOf(':');
        if (idx != -1) {
            m.field = value.substring(0, idx);
            m.fallback = stripQuotes(value.substring(idx + 1));
        } else {
            m.field = value;
        }

        return m;
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }


    private static String normalizeValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String s) {
            if (s.isBlank()) return "";
            if (s.trim().equals("[]") || s.trim().equals("{}")) return "";
            return s;
        }

        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(". "));
        }

        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> STR."\{e.getKey()}: \{e.getValue()}")
                    .collect(Collectors.joining(", "));
        }

        return value.toString();
    }

    public static List<String> buildExactValues(Map<String, Object> metadata) {
        return metadata.values().stream()
                .flatMap(JsonChunkerHelper::flattenMetadataValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static Stream<String> flattenMetadataValue(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .flatMap(JsonChunkerHelper::flattenMetadataValue);
        }

        if (value instanceof Map<?, ?> map) {
            return map.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(JsonChunkerHelper::flattenMetadataValue);
        }

        return Stream.of(String.valueOf(value));
    }



    private static class VariableMapping {
        String field;
        boolean required;
        String fallback;
    }


}
