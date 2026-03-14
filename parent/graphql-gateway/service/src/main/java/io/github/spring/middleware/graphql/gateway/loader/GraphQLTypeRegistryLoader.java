package io.github.spring.middleware.graphql.gateway.loader;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.graphql.gateway.client.IntrospectionGraphQLClient;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GraphQLTypeRegistryLoader {

    private final RegistryClient registryClient;
    private final IntrospectionGraphQLClient introspectionGraphQLClient;
    private final SchemaParser schemaParser = new SchemaParser();

    public GraphQLTypeRegistryMap loadTypeRegistryMap() {
        var typeRegistryMap = registryClient.getSchemaLocations().stream().map(schemaLocation -> {
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
        return new GraphQLTypeRegistryMap(typeRegistryMap);

    }
}
