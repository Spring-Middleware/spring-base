package io.github.spring.middleware.ai.rag.chunk.json;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonChunkRulesLoaderTest {

    @Test
    void shouldLoadCatalogsRules() {
        // Given
        JsonChunkRulesLoader loader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        String rulesPath = "classpath:rules/catalogs.yml";

        // When
        JsonChunkRulesDefinition definition = loader.load(rulesPath);

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.rules()).isNotEmpty().hasSize(1);

        JsonChunkRule catalogRule = definition.rules().getFirst();
        assertThat(catalogRule.name()).isEqualTo("catalog");
        assertThat(catalogRule.extractorPath()).isEqualTo("$.data.catalogs.content[*]");
        assertThat(catalogRule.extractorRules()).hasSize(4);

        assertThat(catalogRule.generationTextRules()).hasSize(1);
        assertThat(catalogRule.generationTextRules().getFirst().template()).isEqualTo("Catalog {catalogName} (id: {catalogId}) is {catalogStatus}.");

        assertThat(catalogRule.children()).hasSize(1);
        JsonChunkRule productRule = catalogRule.children().getFirst();
        assertThat(productRule.name()).isEqualTo("product");
        assertThat(productRule.extractorPath()).isEqualTo("$.products[*]");
        assertThat(productRule.extractorRules()).hasSize(9);
        assertThat(productRule.generationTextRules()).hasSize(5);
        assertThat(productRule.children()).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenFileNotFound() {
        // Given
        JsonChunkRulesLoader loader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        String rulesPath = "classpath:rules/not-found.yml";

        // When & Then
        assertThatThrownBy(() -> loader.load(rulesPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON chunk rules file not found");
    }
}
