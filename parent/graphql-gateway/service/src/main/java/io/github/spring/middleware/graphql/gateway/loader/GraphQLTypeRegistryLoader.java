package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.graphql.gateway.client.IntrospectionGraphQLClient;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.scalars.ScalarsProvider;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GraphQLTypeRegistryLoader {

    private final IntrospectionGraphQLClient introspectionGraphQLClient;
    private final SchemaParser schemaParser = new SchemaParser();
    private final Optional<ScalarsProvider> scalarsProviderOptional;

    public GraphQLTypeRegistryMap loadTypeRegistryMap(List<SchemaLocation> schemaLocations) {
        var typeRegistryMap = schemaLocations.stream().map(schemaLocation -> {
            try {
                String sdl = introspectionGraphQLClient.fetchRemoteSchema(schemaLocation);
                TypeDefinitionRegistry registry = schemaParser.parse(sdl);
                return new AbstractMap.SimpleEntry<>(schemaLocation, registry);
            } catch (Exception e) {
                throw new GraphQLException(
                        GraphQLErrorCodes.SCHEMA_PARSE_ERROR,
                        STR."Failed to load schema for namespace: \{schemaLocation.getNamespace()}",
                        e
                );
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new GraphQLTypeRegistryMap(typeRegistryMap, scalarsProviderOptional);

    }
}
