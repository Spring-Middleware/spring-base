package io.github.spring.middleware.graphql.gateway.client;

import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.util.WebClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import static io.github.spring.middleware.client.proxy.UrlJoiner.join;
import static io.github.spring.middleware.utils.EndpointUtils.joinUrl;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeContextPath;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeEndpoint;
import static io.github.spring.middleware.utils.EndpointUtils.normalizePath;

@Slf4j
@Component
public class GraphQLSchemaMetadataClient {

    private WebClient webClient = WebClientUtils.createWebClient(1000, 5);

    public GraphQLSchemaMetadata fetchSchemaMetadata(SchemaLocation schemaLocation) {
        final String clusterEndpoint = joinUrl(normalizeEndpoint(schemaLocation.getLocation()), normalizeContextPath(schemaLocation.getContextPath()));
        final String graphqlSchemaMetaDataEndpoint = join(clusterEndpoint, normalizePath(STR."\{schemaLocation.getPathApi()}/schema-metadata"));

        try {
            return webClient.get()
                    .uri(graphqlSchemaMetaDataEndpoint)
                    .retrieve()
                    .bodyToMono(GraphQLSchemaMetadata.class)
                    .block();

        } catch (WebClientException webClientException) {
            log.warn("Error fetching schema metadata from endpoint {}: {}", graphqlSchemaMetaDataEndpoint, webClientException.getMessage());
            return null;
        } catch (Exception ex) {
            log.error("Unexpected error fetching schema metadata from endpoint {}: {}", graphqlSchemaMetaDataEndpoint, ex.getMessage(), ex);
            throw new GraphQLException(GraphQLErrorCodes.SCHEMA_METADATA_FETCH_ERROR, STR."Error fetching schema metadata from endpoint \{graphqlSchemaMetaDataEndpoint}", ex);
        }
    }
}
