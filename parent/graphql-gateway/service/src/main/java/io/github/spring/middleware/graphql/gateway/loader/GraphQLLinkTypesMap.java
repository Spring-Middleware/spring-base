package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.GraphQLType;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLRemoteLinkExecutor;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLLinkedType;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GraphQLLinkTypesMap {

    private final Logger log = LoggerFactory.getLogger(GraphQLLinkTypesMap.class);
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

        return resolvedLinkMap.get(new FieldCoordinate(typeName, getParentTypeForTypeName(typeName),  getWrapperTypeNamesForType(typeName), fieldName));
    }

    private List<String> getWrapperTypeNamesForType(String typeName) {
        return linkTypesMap.values().stream()
                .flatMap(linkTypeData -> linkTypeData.getWrapperTypeNamesForType(typeName).stream())
                .distinct()
                .toList();
    }

    public String getParentTypeForTypeName(String typeName) {
        return linkTypesMap.values().stream()
                .map(linkTypeData -> linkTypeData.getParentTypeNameForType(typeName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
                if (typeName == null || coordinate.typeName().equals(typeName) || coordinate.wrappedTypeNames().contains(typeName) || coordinate.parentTypeName().equals(typeName)) {
                    resolvedLinks.add(resolvedLink);
                }
            }
        });
        return resolvedLinks;
    }

    public GraphQLLinkedType findLinkedTypeByTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }

        return linkTypesMap.values().stream()
                .flatMap(linkTypeData -> linkTypeData.linkedTypes().stream())
                .filter(linkedType -> typeName.equals(linkedType.getTypeName()))
                .findFirst()
                .orElse(null);
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
                    FieldCoordinate coordinate = new FieldCoordinate(typeName, linkedType.getParentTypeName(), linkedType.getWrapperTypeNames(), fieldName);

                    SchemaLocation targetSchemaLocation = namespaceToSchemaLocationMap.get(fieldLinkDefinition.getSchema());
                    if (targetSchemaLocation != null) {
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
                    }else{
                        log.warn(
                                "No target schema location found for linked field definition with coordinate [%s.%s] in service [%s]. Skipping this linked field."
                                        .formatted(typeName, fieldName, serviceName)
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

        public List<String> getWrapperTypeNamesForType(String typeName) {
            return linkedTypes.stream()
                    .filter(linkedType -> linkedType.getTypeName().equals(typeName))
                    .flatMap(linkedType -> linkedType.getWrapperTypeNames().stream())
                    .toList();
        }

        public String getParentTypeNameForType(String typeName) {
            return linkedTypes.stream()
                    .filter(linkedType -> linkedType.getTypeName().equals(typeName))
                    .map(GraphQLLinkedType::getParentTypeName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

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
        private Map<String, GraphQLType> targetFieldArgumentTypes;

        public String getFieldName() {
            return fieldLinkDefinition.getFieldName();
        }

        public void addTargetFieldArgumentType(String argumentName, GraphQLType argumentType) {
            if (targetFieldArgumentTypes == null) {
                targetFieldArgumentTypes = new LinkedHashMap<>();
            }
            targetFieldArgumentTypes.put(argumentName, argumentType);
        }

        public boolean isBatched() {
            return fieldLinkDefinition.isBatched();
        }

        public boolean isBatcheArgument(String argumentName) {
            if (fieldLinkDefinition.getArgumentLinkDefinitions() == null) {
                return false;
            }
            return fieldLinkDefinition.getArgumentLinkDefinitions().stream()
                    .filter(argDef -> argDef.getArgumentName().equals(argumentName))
                    .anyMatch(GraphQLArgumentLinkDefinition::isBatched);
        }

        public BatchKey getBatchKey(Map<String, Object> variables) {
            Map<String, Object> args = fieldLinkDefinition.getArgumentLinkDefinitions().stream()
                    .filter(GraphQLArgumentLinkDefinition::isBatched)
                    .sorted(Comparator.comparing(GraphQLArgumentLinkDefinition::getArgumentName))
                    .filter(argDef -> {
                        Object value = variables.get(argDef.getArgumentName());
                        if (value == null) {
                            return false;
                        }
                        if (value instanceof Collection<?> collection) {
                            return !collection.isEmpty();
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(
                            GraphQLArgumentLinkDefinition::getArgumentName,
                            argDef -> variables.get(argDef.getArgumentName()),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            return new BatchKey(fieldLinkDefinition.getQuery(), args);
        }
    }

    public boolean isFieldLinked(String typeName, String fieldName) {
        if (resolvedLinkMap == null) {
            buildResolvedLinkMap();
        }
        return resolvedLinkMap.containsKey(new FieldCoordinate(typeName, getParentTypeForTypeName(typeName), getWrapperTypeNamesForType(typeName), fieldName));
    }


    public record FieldCoordinate(
            String typeName,
            String parentTypeName,
            List<String> wrappedTypeNames,
            String fieldName
    ) {
    }

    public record BatchKey(String query, Map<String, Object> args) {

        public List<GraphQLRemoteLinkExecutor.ItemKey> splitIntoItemKeys(GraphQLResolvedLink resolvedLink) {
            List<String> batchedArgumentNames = resolvedLink.getFieldLinkDefinition()
                    .getArgumentLinkDefinitions().stream()
                    .filter(GraphQLArgumentLinkDefinition::isBatched)
                    .map(GraphQLArgumentLinkDefinition::getArgumentName)
                    .sorted()
                    .toList();

            if (batchedArgumentNames.isEmpty()) {
                throw new IllegalStateException(
                        "No batched arguments defined for query '%s'".formatted(query)
                );
            }

            Map<String, List<?>> batchedValues = new LinkedHashMap<>();

            for (String argumentName : batchedArgumentNames) {
                Object value = args.get(argumentName);

                if (value == null) {
                    throw new IllegalStateException(
                            "Missing batch argument '%s' for query '%s'".formatted(argumentName, query)
                    );
                }

                if (!(value instanceof Collection<?> collection)) {
                    throw new IllegalStateException(
                            "Batch argument '%s' for query '%s' must be a collection".formatted(argumentName, query)
                    );
                }

                if (collection.isEmpty()) {
                    throw new IllegalStateException(
                            "Batch argument '%s' for query '%s' cannot be an empty collection".formatted(argumentName, query)
                    );
                }

                batchedValues.put(argumentName, List.copyOf(collection));
            }

            int expectedSize = batchedValues.values().iterator().next().size();

            for (Map.Entry<String, List<?>> entry : batchedValues.entrySet()) {
                if (entry.getValue().size() != expectedSize) {
                    throw new IllegalStateException(
                            "All batch argument collections for query '%s' must have the same size. Argument '%s' has size %d but expected %d"
                                    .formatted(query, entry.getKey(), entry.getValue().size(), expectedSize)
                    );
                }
            }

            List<GraphQLRemoteLinkExecutor.ItemKey> itemKeys = new ArrayList<>(expectedSize);

            for (int i = 0; i < expectedSize; i++) {
                Map<String, Object> itemArgs = new LinkedHashMap<>();

                for (String argumentName : batchedArgumentNames) {
                    itemArgs.put(argumentName, batchedValues.get(argumentName).get(i));
                }

                itemKeys.add(new GraphQLRemoteLinkExecutor.ItemKey(query, itemArgs));
            }

            return itemKeys;
        }
    }
}