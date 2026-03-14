package io.github.spring.middleware.graphql.gateway.merger;

import graphql.language.FieldDefinition;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Getter
public class GraphQLMerged {

    private final Map<GraphQLOperationKey, SchemaLocation> operationKeySchemaLocationMap;
    private final Map<SchemaLocation, List<GraphQLOperation>> operationsByLocation;

    public GraphQLMerged(
            Map<GraphQLOperationKey, SchemaLocation> operationKeySchemaLocationMap,
            Map<SchemaLocation, List<GraphQLOperation>> operationsByLocation
    ) {
        this.operationKeySchemaLocationMap = operationKeySchemaLocationMap;
        this.operationsByLocation = operationsByLocation;
    }

    public List<FieldDefinition> getFieldDefinitionsByOperationType(GraphQLOperationType operationType) {
        return operationsByLocation.entrySet().stream().map(Map.Entry::getValue).flatMap(List::stream)
                .filter(op -> op.getType() == operationType)
                .map(GraphQLOperation::getFieldDefinition).toList();
    }

    public FieldDefinition getFieldDefinition(GraphQLOperationKey operationKey) {
        SchemaLocation schemaLocation = operationKeySchemaLocationMap.get(operationKey);
        if (schemaLocation == null) {
            throw new IllegalArgumentException(STR."No schema location found for operation key: \{operationKey}");
        }
        return operationsByLocation.get(schemaLocation).stream()
                .filter(op -> op.getKey().equals(operationKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(STR."No operation found for key: \{operationKey}"))
                .getFieldDefinition();
    }

    public List<GraphQLOperationKey> getOperationKeysByType(GraphQLOperationType operationType) {
        return operationsByLocation.entrySet().stream().map(Map.Entry::getValue).flatMap(List::stream)
                .filter(op -> op.getType() == operationType)
                .map(GraphQLOperation::getKey).toList();
    }

}
