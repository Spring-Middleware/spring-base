package io.github.spring.middleware.graphql.gateway.loader;

import graphql.language.UnionTypeDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.graphql.gateway.scalars.ScalarsProvider;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.spring.middleware.graphql.gateway.util.RuntimeWiringUtils.registerResolver;
import static io.github.spring.middleware.graphql.gateway.util.RuntimeWiringUtils.registerScalars;


public record GraphQLTypeRegistryMap(Map<SchemaLocation, TypeDefinitionRegistry> registryMap, Optional<ScalarsProvider> scalarsProviderOptional) {

    private static Map<String, SchemaLocation> typeNameToLocationMap = new HashMap<>();
    private static Map<SchemaLocation, GraphQLSchema> executableSchemaCache = new HashMap<>();

    public boolean isEmpty() {
        return registryMap.isEmpty();
    }


    public GraphQLFieldDefinition getGraphQLFieldDefinition(
            SchemaLocation schemaLocation,
            String targetOperationName,
            GraphQLLinkTypesMap graphQLLinkTypesMap
    ) {
        GraphQLSchema schema = getExecutableSchema(schemaLocation, graphQLLinkTypesMap);

        GraphQLObjectType rootType = schema.getObjectType("Query");
        if (rootType == null) {
            throw new IllegalArgumentException(STR."Root type not found: Query}");
        }

        GraphQLFieldDefinition fieldDefinition = rootType.getFieldDefinition(targetOperationName);
        if (fieldDefinition == null) {
            throw new IllegalArgumentException(
                    STR."Field not found: Query.\{targetOperationName}"
            );
        }

        return fieldDefinition;
    }




    public GraphQLType getGraphQLTypeForFieldInType(SchemaLocation schemaLocation, String typeName, String fieldName, GraphQLLinkTypesMap graphQLLinkTypesMap) {
        GraphQLSchema schema = getExecutableSchema(schemaLocation, graphQLLinkTypesMap);
        GraphQLType type = schema.getType(typeName);
        if (!(type instanceof GraphQLObjectType objectType)) {
            throw new IllegalArgumentException(
                    STR."Type is not an object: \{typeName}"
            );
        }

        GraphQLFieldDefinition fieldDefinition = objectType.getFieldDefinition(fieldName);
        if (fieldDefinition == null) {
            throw new IllegalArgumentException(
                    STR."Field not found: \{typeName}.\{fieldName}"
            );
        }

        return fieldDefinition.getType();
    }

    private GraphQLSchema getExecutableSchema(SchemaLocation schemaLocation, GraphQLLinkTypesMap graphQLLinkTypesMap) {
        if (executableSchemaCache.containsKey(schemaLocation)) {
            return executableSchemaCache.get(schemaLocation);
        }
        TypeDefinitionRegistry registry = registryMap.get(schemaLocation);
        if (registry == null) {
            throw new IllegalArgumentException(STR."No registry found for schema location: \{schemaLocation}");
        }
        GraphQLSchema executableSchema = buildExecutableSchema(registry, graphQLLinkTypesMap);
        executableSchemaCache.put(schemaLocation, executableSchema);
        return executableSchema;
    }

    private GraphQLSchema buildExecutableSchema(TypeDefinitionRegistry registry, GraphQLLinkTypesMap graphQLLinkTypesMap) {
        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
        registerScalars(runtimeWiringBuilder, scalarsProviderOptional);
        registry.getTypes(UnionTypeDefinition.class).forEach(unionTypeDefinition -> registerResolver(runtimeWiringBuilder, unionTypeDefinition));
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(registry, runtimeWiringBuilder.build());
    }

}
