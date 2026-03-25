package io.github.spring.middleware.graphql.gateway.loader;

import io.github.spring.middleware.graphql.gateway.client.GraphQLSchemaMetadataClient;
import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static io.github.spring.middleware.client.proxy.UrlJoiner.join;
import static io.github.spring.middleware.utils.EndpointUtils.joinUrl;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeContextPath;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeEndpoint;
import static io.github.spring.middleware.utils.EndpointUtils.normalizePath;

@Component
@RequiredArgsConstructor
public class GraphQLSchemaMetadataLoader {

    private final GraphQLSchemaMetadataClient schemaMetadataClient;

    public GraphQLLinkTypesMap loadGraphQLLinkTypesMap(Collection<SchemaLocation> schemaLocations) {
        GraphQLLinkTypesMap linkTypesMap = new GraphQLLinkTypesMap();

        schemaLocations.forEach(schemaLocation -> {
            GraphQLSchemaMetadata schemaMetadata = schemaMetadataClient.fetchSchemaMetadata(schemaLocation);
            if (schemaMetadata == null || schemaMetadata.getGraphQLLinkedTypes() == null || schemaMetadata.getGraphQLLinkedTypes().isEmpty()) {
                return;
            }

            linkTypesMap.addLinkTypes(
                    schemaLocation.getNamespace(),
                    buildGraphQLEndpoint(schemaLocation),
                    schemaMetadata.getGraphQLLinkedTypes()
            );
        });

        return linkTypesMap;
    }


    private String buildGraphQLEndpoint(SchemaLocation schemaLocation) {
        final String clusterEndpoint = joinUrl(normalizeEndpoint(schemaLocation.getLocation()), normalizeContextPath(schemaLocation.getContextPath()));
        return join(clusterEndpoint, normalizePath(schemaLocation.getPathApi()));
    }


}
