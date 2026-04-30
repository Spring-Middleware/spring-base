package io.github.spring.middleware.ai.rag.chunk.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonChunkRulesLoader {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlObjectMapper;

    public JsonChunkRulesLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .findAndRegisterModules();
    }

    public JsonChunkRulesDefinition load(String rulesPath) {
        Resource resource = resourceLoader.getResource(rulesPath);

        if (!resource.exists()) {
            throw new IllegalArgumentException(STR."JSON chunk rules file not found: \{rulesPath}");
        }

        try (var inputStream = resource.getInputStream()) {
            return yamlObjectMapper.readValue(inputStream, JsonChunkRulesDefinition.class);
        } catch (IOException e) {
            throw new IllegalStateException(STR."Could not load JSON chunk rules from: \{rulesPath}", e);
        }
    }
}
