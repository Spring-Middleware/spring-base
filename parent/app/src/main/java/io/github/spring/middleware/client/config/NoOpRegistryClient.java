package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NoOpRegistryClient implements RegistryClient  {

    @Override
    public void registerResource(ResourceRegisterParameters resourceRegisterParameters) {
        log.warn(STR."NoOpRegistryClient: registerResource called with parameters: \{resourceRegisterParameters}");
    }

    @Override
    public void registerGraphQLSchemaLocation(SchemaRegisterParameters schemaRegisterParameters) {
        log.warn(STR."NoOpRegistryClient: registerGraphQLSchemaLocation called with parameters: \{schemaRegisterParameters}");
    }

    @Override
    public void deleteGraphQLSchemaLocation(String namespace) {
        log.warn(STR."NoOpRegistryClient: deleteGraphQLSchemaLocation called with namespace: \{namespace}");
    }

    @Override
    public RegistryEntry getRegistryEntry(String resource) {
        log.warn(STR."NoOpRegistryClient: getRegistryEntry called with resource: \{resource}");
        return null;
    }

    @Override
    public RegistryMap getRegistryMap() {
        return null;
    }

    @Override
    public List<SchemaLocation> getSchemaLocations() {
        return List.of();
    }

    @Override
    public SchemaLocation getSchemaLocation(String namespace) {
        return null;
    }
}
