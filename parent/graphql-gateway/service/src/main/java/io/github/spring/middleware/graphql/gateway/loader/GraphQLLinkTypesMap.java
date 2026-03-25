package io.github.spring.middleware.graphql.gateway.loader;

import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLLinkedType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class GraphQLLinkTypesMap {

    private final Map<String, GraphQLLinkTypeData> linkTypesMap = new LinkedHashMap<>();
    private Map<FieldCoordinate, GraphQLResolvedLink> resolvedLinkMap;

    public void addLinkTypes(String serviceName, String graphQLEndpoint, Collection<GraphQLLinkedType> linkedTypes) {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(graphQLEndpoint, "graphQLEndpoint must not be null");
        Objects.requireNonNull(linkedTypes, "linkedTypes must not be null");

        linkTypesMap.put(serviceName, new GraphQLLinkTypeData(graphQLEndpoint, linkedTypes));
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

    public boolean isEmpty() {
        return linkTypesMap.isEmpty();
    }

    private void buildResolvedLinkMap() {
        Map<FieldCoordinate, GraphQLResolvedLink> resolvedLinks = new LinkedHashMap<>();

        linkTypesMap.forEach((serviceName, linkTypeData) -> {
            String graphQLEndpoint = linkTypeData.graphQLEndpoint();

            linkTypeData.linkedTypes().forEach(linkedType -> {
                String typeName = linkedType.getTypeName();

                if (linkedType.getGraphQLFieldLinkDefinitions() == null) {
                    return;
                }

                linkedType.getGraphQLFieldLinkDefinitions().forEach(fieldLinkDefinition -> {
                    String fieldName = fieldLinkDefinition.getFieldName();
                    FieldCoordinate coordinate = new FieldCoordinate(typeName, fieldName);

                    GraphQLResolvedLink newResolvedLink =
                            new GraphQLResolvedLink(serviceName, graphQLEndpoint, fieldLinkDefinition);

                    GraphQLResolvedLink existingResolvedLink = resolvedLinks.putIfAbsent(coordinate, newResolvedLink);
                    if (existingResolvedLink != null) {
                        throw new IllegalStateException(
                                "Duplicated GraphQL linked field definition for coordinate [%s.%s]. "
                                        .formatted(typeName, fieldName)
                                        + "Already registered by service [%s] and attempted again by service [%s]"
                                        .formatted(existingResolvedLink.serviceName(), serviceName)
                        );
                    }
                });
            });
        });

        this.resolvedLinkMap = resolvedLinks;
    }

    public record GraphQLLinkTypeData(
            String graphQLEndpoint,
            Collection<GraphQLLinkedType> linkedTypes
    ) {
    }

    public record GraphQLResolvedLink(
            String serviceName,
            String graphQLEndpoint,
            GraphQLFieldLinkDefinition fieldLinkDefinition
    ) {
    }

    public record FieldCoordinate(
            String typeName,
            String fieldName
    ) {
    }
}