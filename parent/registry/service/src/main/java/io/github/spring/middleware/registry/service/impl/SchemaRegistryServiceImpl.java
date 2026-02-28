package io.github.spring.middleware.registry.service.impl;

import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import io.github.spring.middleware.registry.service.SchemaRegistryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaRegistryServiceImpl implements SchemaRegistryService {
    @Override
    public void registerSchemaLocation(SchemaRegisterParameters schemaRegisterParameters) {

    }

    @Override
    public List<SchemaLocation> getSchemaLocations() {
        return List.of();
    }

    @Override
    public void removeSchemaLocation(String namespace) {

    }

    @Override
    public void removeSchemaLocationNode(String namespace, String locationNode) {

    }

    @Override
    public SchemaLocation getSchemaLocation(String namespace) {
        return null;
    }
}
