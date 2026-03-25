package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.HashMap;
import java.util.Map;


public record GraphQLTypeRegistryMap(Map<SchemaLocation, TypeDefinitionRegistry> registryMap) {

    private static Map<String, SchemaLocation> typeNameToLocationMap = new HashMap<>();

    public boolean isEmpty() {
        return registryMap.isEmpty();
    }

}
