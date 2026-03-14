package io.github.spring.middleware.graphql.gateway.merger;

import graphql.language.FieldDefinition;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryMap;
import io.github.spring.middleware.registry.model.SchemaLocation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GraphQLTypeRegistryMerger {

    public GraphQLMerged merge(GraphQLTypeRegistryMap typeRegistryMap) {

        Map<GraphQLOperationKey, SchemaLocation> operationKeySchemaLocationMap = new HashMap<>();
        Map<SchemaLocation, List<GraphQLOperation>> operationsByLocation = new HashMap<>();

        typeRegistryMap.registryMap().forEach((schemaLocation, definitionRegistry) -> {
            GraphQLRootTypeResolver.getQuery(definitionRegistry).ifPresent(query ->
                    query.getFieldDefinitions().forEach(fieldDefinition ->
                            addGraphQLOperation(
                                    operationsByLocation,
                                    operationKeySchemaLocationMap,
                                    schemaLocation,
                                    GraphQLOperationType.QUERY,
                                    fieldDefinition
                            )
                    )
            );

            GraphQLRootTypeResolver.getMutation(definitionRegistry).ifPresent(mutation ->
                    mutation.getFieldDefinitions().forEach(fieldDefinition ->
                            addGraphQLOperation(
                                    operationsByLocation,
                                    operationKeySchemaLocationMap,
                                    schemaLocation,
                                    GraphQLOperationType.MUTATION,
                                    fieldDefinition
                            )
                    )
            );
        });

        return new GraphQLMerged(operationKeySchemaLocationMap, operationsByLocation);
    }

    private void addGraphQLOperation(
            Map<SchemaLocation, List<GraphQLOperation>> operationsByLocation,
            Map<GraphQLOperationKey, SchemaLocation> operationKeySchemaLocationMap,
            SchemaLocation schemaLocation,
            GraphQLOperationType type,
            FieldDefinition fieldDefinition
    ) {
        GraphQLOperation operation = new GraphQLOperation(type, fieldDefinition.getName(), fieldDefinition);
        GraphQLOperationKey operationKey = operation.getKey();

        SchemaLocation existingSchemaLocation = operationKeySchemaLocationMap.get(operationKey);
        if (existingSchemaLocation != null) {
            throw new GraphQLException(
                    GraphQLErrorCodes.SCHEMA_MERGE_ERROR,
                    STR."Operation conflict detected for operation: \{type}.\{fieldDefinition.getName()} in schema: \{schemaLocation.getNamespace()}. Already defined in schema: \{existingSchemaLocation.getNamespace()}"
            );
        }

        operationKeySchemaLocationMap.put(operationKey, schemaLocation);

        operationsByLocation
                .computeIfAbsent(schemaLocation, key -> new ArrayList<>())
                .add(operation);
    }
}