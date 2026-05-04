package io.github.spring.middleware.ai.rag.chunk.json;

import java.util.List;

public record JsonChunkRule(String name,
                            String extractorPath,      // JsonPath: $.data.catalogs.content[*].products[*]
                            List<String> inheritMetadata,
                            List<JsonChunkExtractorRule> extractorRules,
                            List<JsonChunkGenerationTextRule> generationTextRules,
                            // extract information from objects obtained in selectorPaths
                            List<JsonChunkRule> children

) {
}
