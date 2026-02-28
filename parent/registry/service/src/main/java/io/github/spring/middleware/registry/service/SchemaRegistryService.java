package io.github.spring.middleware.registry.service;

import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;

import java.util.List;

public interface SchemaRegistryService {

    void registerSchemaLocation(SchemaRegisterParameters schemaRegisterParameters);

    List<SchemaLocation> getSchemaLocations();

    void removeSchemaLocation(String namespace);

    void removeSchemaLocationNode(String namespace, String locationNode);

    SchemaLocation getSchemaLocation(String namespace);

}
