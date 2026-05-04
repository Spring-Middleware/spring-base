package io.github.spring.middleware.ai.rag.chunk.json;

import io.github.spring.middleware.ai.rag.chunk.DocumentChunkInput;
import io.github.spring.middleware.ai.rag.chunk.config.DocumentChunkerConfiguration;
import io.github.spring.middleware.ai.rag.source.DocumentSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonChunkerTest {

    @Test
    void shouldGenerateChunksFromCatalogs() throws Exception {
        // Given
        JsonChunkRulesLoader rulesLoader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        JsonChunker chunker = new JsonChunker(rulesLoader, new DocumentChunkerConfiguration());

        InputStream jsonInputStream = new DefaultResourceLoader().getResource("classpath:data/catalogs.json").getInputStream();
        DocumentSource documentSource = new DocumentSource(
                "doc-123",
                "Catalogs",
                jsonInputStream,
                "json",
                "application/json",
                Map.of(),
                Instant.now()
        );

        JsonChunkerOptions options = new JsonChunkerOptions(List.of("classpath:rules/catalogs.yml"));

        // When
        Flux<DocumentChunkInput> chunksFlux = chunker.chunk(documentSource, options);

        List<DocumentChunkInput> chunks = chunksFlux.collectList().block();

        // Then
        assertThat(chunks).hasSize(6);

        DocumentChunkInput catalogChunk = chunks.get(0);
        assertThat(catalogChunk.content()).startsWith("Catalog Summer Collection (id: 11111111-1111-1111-1111-111111111111) is ACTIVE.");
        assertThat(catalogChunk.content()).contains("Best summer items");
        assertThat(catalogChunk.metadata())
                .containsEntry("catalogId", "11111111-1111-1111-1111-111111111111")
                .containsEntry("catalogName", "Summer Collection")
                .containsEntry("catalogStatus", "ACTIVE");

        DocumentChunkInput productChunk = chunks.get(1);
        assertThat(productChunk.content()).contains("Product Sunglasses belongs to catalog Summer Collection.");
        assertThat(productChunk.content()).contains("It is a PhysicalProduct with price 25.5 USD.");
        assertThat(productChunk.content()).contains("Description: UV400 sun protection glasses.");
        assertThat(productChunk.content()).contains("Reviews: [\"Great glasses!\",\"A bit scratched\"]");
        assertThat(productChunk.content()).contains("Dimensions: 15.0x5.0x4.0.");

        assertThat(productChunk.metadata())
                .containsEntry("productId", "11111111-1111-1111-1111-111111111112")
                .containsEntry("productName", "Sunglasses")
                .containsEntry("productType", "PhysicalProduct");
                
        DocumentChunkInput productChunk2 = chunks.get(2);
        assertThat(productChunk2.content()).contains("Product Beach Sounds MP3 belongs to catalog Summer Collection.");
        assertThat(productChunk2.content()).contains("It is a DigitalProduct with price 5.0 USD.");
        assertThat(productChunk2.content()).contains("Reviews: [\"Very relaxing\"]");
        assertThat(productChunk2.content()).contains("Download digital asset at https://downloads.example.com/beach-sounds.mp3 (15420 bytes).");
        assertThat(productChunk2.content()).doesNotContain("Dimensions:");
        assertThat(productChunk2.content()).doesNotContain("Description:");
        assertThat(productChunk2.metadata())
                .containsEntry("productId", "11111111-1111-1111-1111-111111111113")
                .containsEntry("productName", "Beach Sounds MP3")
                .containsEntry("productType", "DigitalProduct");
                
        DocumentChunkInput catalogChunk2 = chunks.get(3);
        assertThat(catalogChunk2.content()).startsWith("Catalog Office Collection (id: 22222222-2222-2222-2222-222222222222) is ACTIVE.");
        assertThat(catalogChunk2.content()).contains("Premium office gear");
        assertThat(catalogChunk2.metadata())
                .containsEntry("catalogId", "22222222-2222-2222-2222-222222222222")
                .containsEntry("catalogName", "Office Collection")
                .containsEntry("catalogStatus", "ACTIVE");

        DocumentChunkInput productChunk3 = chunks.get(4);
        assertThat(productChunk3.content()).contains("Product USB-C Docking Station belongs to catalog Office Collection.");
        assertThat(productChunk3.content()).contains("It is a PhysicalProduct with price 898.28 EUR.");
        assertThat(productChunk3.content()).contains("Description: Docking station with multi-port connectivity for laptops and desktops.");
        assertThat(productChunk3.content()).contains("Reviews: [\"Works really well overall.\",\"Excellent product!\"]");
        assertThat(productChunk3.content()).contains("Dimensions: 62.6x10.4x6.3.");
        assertThat(productChunk3.content()).doesNotContain("Download digital asset");
        assertThat(productChunk3.metadata())
                .containsEntry("productId", "4cba9446-6b4f-4e10-91e0-35e97770dc55")
                .containsEntry("productName", "USB-C Docking Station")
                .containsEntry("productType", "PhysicalProduct");

        DocumentChunkInput productChunk4 = chunks.get(5);
        assertThat(productChunk4.content()).contains("Product Testing Strategies Masterclass belongs to catalog Office Collection.");
        assertThat(productChunk4.content()).contains("It is a DigitalProduct with price 132.44 EUR.");
        assertThat(productChunk4.content()).contains("Reviews: [\"Really happy with this purchase.\"]");
        assertThat(productChunk4.content()).contains("Download digital asset at https://downloads.example.com/testing-strategies-masterclass-8455.mp4 (22370 bytes).");
        assertThat(productChunk4.content()).doesNotContain("Dimensions:");
        assertThat(productChunk4.content()).doesNotContain("Description:");
        assertThat(productChunk4.metadata())
                .containsEntry("productId", "aa363032-8515-413c-a24a-bd93cf552e8d")
                .containsEntry("productName", "Testing Strategies Masterclass")
                .containsEntry("productType", "DigitalProduct");
    }

    @Test
    void shouldApplyFallbackWhenRequiredVariableIsMissing() throws Exception {
        // Given
        JsonChunkRulesLoader rulesLoader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        JsonChunker chunker = new JsonChunker(rulesLoader, new DocumentChunkerConfiguration());

        String jsonPayload = """
        {
          "data": {
            "catalogs": {
              "content": [
                {
                  "id": "1111",
                  "name": "Test Base",
                  "products": [
                    {
                      "id": "1",
                      "name": "MissingTypeProduct",
                      "price": { "amount": 10.0, "currency": "USD" }
                    }
                  ]
                }
              ]
            }
          }
        }
        """;
        
        java.io.ByteArrayInputStream jsonInputStream = new java.io.ByteArrayInputStream(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        DocumentSource documentSource = new DocumentSource(
                "doc-test",
                "Catalogs",
                jsonInputStream,
                "json",
                "application/json",
                Map.of(),
                Instant.now()
        );

        JsonChunkerOptions options = new JsonChunkerOptions(List.of("classpath:rules/catalogs.yml"));

        // When
        Flux<DocumentChunkInput> chunksFlux = chunker.chunk(documentSource, options);
        List<DocumentChunkInput> chunks = chunksFlux.collectList().block();

        // Then
        assertThat(chunks).isNotEmpty();
        DocumentChunkInput productChunk = chunks.get(1); // 0 is catalog, 1 is product
        assertThat(productChunk.content()).contains("It is a product with price 10.0 USD.");
    }

    @Test
    void shouldNotRenderTemplateWhenAllOptionalVariablesAreMissing() throws Exception {
        // Given
        JsonChunkRulesLoader rulesLoader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        JsonChunker chunker = new JsonChunker(rulesLoader, new DocumentChunkerConfiguration());

        String jsonPayload = """
        {
          "data": {
            "catalogs": {
              "content": [
                {
                  "id": "1111",
                  "name": "Test Base",
                  "products": [
                    {
                      "id": "1",
                      "name": "MinimalProduct",
                      "price": { "amount": 10.0, "currency": "USD" }
                    }
                  ]
                }
              ]
            }
          }
        }
        """;

        java.io.ByteArrayInputStream jsonInputStream = new java.io.ByteArrayInputStream(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        DocumentSource documentSource = new DocumentSource(
                "doc-test",
                "Catalogs",
                jsonInputStream,
                "json",
                "application/json",
                Map.of(),
                Instant.now()
        );

        JsonChunkerOptions options = new JsonChunkerOptions(List.of("classpath:rules/catalogs.yml"));

        // When
        Flux<DocumentChunkInput> chunksFlux = chunker.chunk(documentSource, options);
        List<DocumentChunkInput> chunks = chunksFlux.collectList().block();

        // Then
        assertThat(chunks).isNotEmpty();
        DocumentChunkInput productChunk = chunks.get(1);

        // These texts shouldn't be present because variables were missing and the whole line rendering is skipped
        assertThat(productChunk.content()).doesNotContain("Description:");
        assertThat(productChunk.content()).doesNotContain("Reviews:");
        assertThat(productChunk.content()).doesNotContain("Dimensions:");
        assertThat(productChunk.content()).doesNotContain("Download digital asset");
    }

    @Test
    void shouldNotRenderTemplateWhenRequiredVariableIsMissing() throws Exception {
        // Given
        JsonChunkRulesLoader rulesLoader = new JsonChunkRulesLoader(new DefaultResourceLoader());
        JsonChunker chunker = new JsonChunker(rulesLoader, new DocumentChunkerConfiguration());

        String jsonPayload = """
        {
          "data": {
            "catalogs": {
              "content": [
                {
                  "id": "1111",
                  "name": "Test Base",
                  "products": [
                    {
                      "id": "1",
                      "price": { "currency": "USD" },
                      "description": "It has description but missing name and price amount"
                    }
                  ]
                }
              ]
            }
          }
        }
        """;

        java.io.ByteArrayInputStream jsonInputStream = new java.io.ByteArrayInputStream(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        DocumentSource documentSource = new DocumentSource(
                "doc-test",
                "Catalogs",
                jsonInputStream,
                "json",
                "application/json",
                Map.of(),
                Instant.now()
        );

        JsonChunkerOptions options = new JsonChunkerOptions(List.of("classpath:rules/catalogs.yml"));

        // When
        Flux<DocumentChunkInput> chunksFlux = chunker.chunk(documentSource, options);
        List<DocumentChunkInput> chunks = chunksFlux.collectList().block();

        // Then
        assertThat(chunks).isNotEmpty();
        DocumentChunkInput productChunk = chunks.get(1);

        // This template requires productName!, so it should be skipped
        assertThat(productChunk.content()).doesNotContain("Product ");
        assertThat(productChunk.content()).doesNotContain("belongs to catalog");

        // This template requires priceAmount!, so it should be skipped
        assertThat(productChunk.content()).doesNotContain("It is a");

        // It SHOULD contain the description because it has no required constraints
        assertThat(productChunk.content()).contains("Description: It has description but missing name and price amount");
    }
}
