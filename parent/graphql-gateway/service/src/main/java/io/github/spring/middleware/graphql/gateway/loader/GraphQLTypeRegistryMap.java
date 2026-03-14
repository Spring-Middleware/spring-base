package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.Map;


public record GraphQLTypeRegistryMap(Map<SchemaLocation, TypeDefinitionRegistry> registryMap) {
}
