package io.github.spring.middleware.graphql.gateway.loader;

import io.github.spring.middleware.graphql.gateway.client.GraphQLSchemaMetadataClient;
import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GraphQLSchemaMetadataLoader {

    private final GraphQLSchemaMetadataClient schemaMetadataClient;

    public GraphQLLinkTypesMap loadGraphQLLinkTypesMap(Collection<SchemaLocation> schemaLocations) {
        final var namespaceToSchemaLocationMap = schemaLocations.stream()
                .collect(Collectors.toMap(SchemaLocation::getNamespace, Function.identity()));
        GraphQLLinkTypesMap linkTypesMap = new GraphQLLinkTypesMap(namespaceToSchemaLocationMap);

        schemaLocations.forEach(schemaLocation -> {
            GraphQLSchemaMetadata schemaMetadata = schemaMetadataClient.fetchSchemaMetadata(schemaLocation);
            if (schemaMetadata == null || schemaMetadata.getGraphQLLinkedTypes() == null || schemaMetadata.getGraphQLLinkedTypes().isEmpty()) {
                return;
            }

            linkTypesMap.addLinkTypes(
                    schemaLocation.getNamespace(),
                    schemaLocation,
                    schemaMetadata.getGraphQLLinkedTypes()
            );
        });

        return linkTypesMap;
    }

}
