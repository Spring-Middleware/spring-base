package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.GraphQLType;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLLinkedType;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GraphQLLinkTypesMap {

    private final Map<String, GraphQLLinkTypeData> linkTypesMap = new LinkedHashMap<>();
    private Map<FieldCoordinate, GraphQLResolvedLink> resolvedLinkMap;
    private Map<String, SchemaLocation> namespaceToSchemaLocationMap;

    public GraphQLLinkTypesMap(Map<String, SchemaLocation> namespaceToSchemaLocationMap) {
        this.namespaceToSchemaLocationMap = namespaceToSchemaLocationMap;
    }

    public void addLinkTypes(String serviceName, SchemaLocation schemaLocation, Collection<GraphQLLinkedType> linkedTypes) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(schemaLocation, "schemaLocation must not be null");
        Objects.requireNonNull(linkedTypes, "linkedTypes must not be null");

        linkTypesMap.put(serviceName, new GraphQLLinkTypeData(schemaLocation, linkedTypes));
        resolvedLinkMap = null;
    }

    public GraphQLResolvedLink findGraphQLResolvedLink(String typeName, String fieldName) {
        Objects.requireNonNull(typeName, "typeName must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");

        if (resolvedLinkMap == null) {
            buildResolvedLinkMap();
        }

        return resolvedLinkMap.get(new FieldCoordinate(typeName, fieldName));
    }

    public List<FieldCoordinate> getAllLinkedFieldCoordinates() {
        if (resolvedLinkMap == null) {
            buildResolvedLinkMap();
        }
        return new ArrayList<>(resolvedLinkMap.keySet());
    }

    public List<GraphQLResolvedLink> findGraphQLResolvedLinksForSchemaAndTypeName(SchemaLocation schemaLocation, String typeName) {
        Objects.requireNonNull(schemaLocation, "schemaLocation must not be null");

        if (resolvedLinkMap == null) {
            buildResolvedLinkMap();
        }

        List<GraphQLResolvedLink> resolvedLinks = new ArrayList<>();
        resolvedLinkMap.forEach((coordinate, resolvedLink) -> {
            if (resolvedLink.getSchemaLocation().equals(schemaLocation)) {
                if (typeName == null || coordinate.typeName().equals(typeName)) {
                    resolvedLinks.add(resolvedLink);
                }
            }
        });
        return resolvedLinks;
    }


    public boolean isEmpty() {
        return linkTypesMap.isEmpty();
    }

    private void buildResolvedLinkMap() {
        Map<FieldCoordinate, GraphQLResolvedLink> resolvedLinks = new LinkedHashMap<>();

        linkTypesMap.forEach((serviceName, linkTypeData) -> {
            SchemaLocation schemaLocation = linkTypeData.schemaLocation();

            linkTypeData.linkedTypes().forEach(linkedType -> {
                String typeName = linkedType.getTypeName();

                if (linkedType.getGraphQLFieldLinkDefinitions() == null) {
                    return;
                }

                linkedType.getGraphQLFieldLinkDefinitions().forEach(fieldLinkDefinition -> {
                    String fieldName = fieldLinkDefinition.getFieldName();
                    FieldCoordinate coordinate = new FieldCoordinate(typeName, fieldName);

                    SchemaLocation targetSchemaLocation = namespaceToSchemaLocationMap.get(fieldLinkDefinition.getSchema());

                    GraphQLResolvedLink newResolvedLink =
                            new GraphQLResolvedLink(schemaLocation, targetSchemaLocation, fieldLinkDefinition);

                    GraphQLResolvedLink existingResolvedLink = resolvedLinks.putIfAbsent(coordinate, newResolvedLink);
                    if (existingResolvedLink != null) {
                        throw new IllegalStateException(
                                "Duplicated GraphQL linked field definition for coordinate [%s.%s]. "
                                        .formatted(typeName, fieldName)
                                        + "Already registered by service [%s] and attempted again by service [%s]"
                                        .formatted(existingResolvedLink.getSchemaLocation().getNamespace(), serviceName)
                        );
                    }
                });
            });
        });

        this.resolvedLinkMap = resolvedLinks;
    }

    public record GraphQLLinkTypeData(
            SchemaLocation schemaLocation,
            Collection<GraphQLLinkedType> linkedTypes
    ) {
    }

    @Getter
    @Setter
    public static class GraphQLResolvedLink {

        public GraphQLResolvedLink(SchemaLocation schemaLocation, SchemaLocation targetSchemaLocation, GraphQLFieldLinkDefinition fieldLinkDefinition) {
            this.schemaLocation = schemaLocation;
            this.targetSchemaLocation = targetSchemaLocation;
            this.fieldLinkDefinition = fieldLinkDefinition;
        }

        private SchemaLocation schemaLocation;
        private SchemaLocation targetSchemaLocation;
        private GraphQLFieldLinkDefinition fieldLinkDefinition;
        private GraphQLType originOperationReturnType;
        private Map<String,GraphQLType> targetFieldArgumentTypes;

        public String getFieldName() {
            return fieldLinkDefinition.getFieldName();
        }

        public void addTargetFieldArgumentType(String argumentName, GraphQLType argumentType) {
            if (targetFieldArgumentTypes == null) {
                targetFieldArgumentTypes = new LinkedHashMap<>();
            }
            targetFieldArgumentTypes.put(argumentName, argumentType);
        }

    }


    public record FieldCoordinate(
            String typeName,
            String fieldName
    ) {
    }
}