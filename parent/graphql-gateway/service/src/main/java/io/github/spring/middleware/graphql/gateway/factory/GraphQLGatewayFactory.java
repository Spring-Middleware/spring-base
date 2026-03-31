package io.github.spring.middleware.graphql.gateway.factory;

import graphql.GraphQL;
import graphql.language.UnionTypeDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLBatchExecutor;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLBatchInstrumentation;
import io.github.spring.middleware.graphql.gateway.builder.GraphQLSchemaDefinitionBuilder;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLRemoteLinkExecutor;
import io.github.spring.middleware.graphql.gateway.fetcher.RemoteDelegatingDataFetcher;
import io.github.spring.middleware.graphql.gateway.fetcher.RemoteDelegatingGraphQLLinkDataFetcher;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLSchemaMetadataLoader;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryLoader;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLTypeRegistryMerger;
import io.github.spring.middleware.graphql.gateway.scalars.ScalarsProvider;
import io.github.spring.middleware.graphql.gateway.util.GraphQLTypeResolvers;
import io.github.spring.middleware.registry.model.SchemaLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

import static io.github.spring.middleware.graphql.gateway.util.RuntimeWiringUtils.registerResolver;
import static io.github.spring.middleware.graphql.gateway.util.RuntimeWiringUtils.registerScalars;

@Configuration
@RequiredArgsConstructor
public class GraphQLGatewayFactory {

    private final GraphQLRemoteLinkExecutor graphQLRemoteLinkExecutor;
    private final RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;
    private final GraphQLTypeRegistryMerger typeRegistryMerger;
    private final GraphQLSchemaDefinitionBuilder schemaDefinitionBuilder;
    private final GraphQLTypeRegistryLoader registryLoader;
    private final GraphQLSchemaMetadataLoader schemaMetadataLoader;
    private final Optional<ScalarsProvider> scalarsProviderOptional;
    private final GraphQLBatchExecutor graphQLBatchExecutor;
    private final RegistryClient registryClient;

    public GraphQL build() {
        List<SchemaLocation> schemaLocations = registryClient.getSchemaLocations();
        final GraphQLTypeRegistryMap graphQLTypeRegistryMap = registryLoader.loadTypeRegistryMap(schemaLocations);
        final GraphQLLinkTypesMap graphQLLinkTypesMap = schemaMetadataLoader.loadGraphQLLinkTypesMap(schemaLocations);
        if (!graphQLTypeRegistryMap.isEmpty()) {
            final GraphQLMerged graphQLMerged = typeRegistryMerger.merge(graphQLTypeRegistryMap);
            final TypeDefinitionRegistry schemaRegistry = schemaDefinitionBuilder.build(graphQLMerged, graphQLTypeRegistryMap, graphQLLinkTypesMap);
            final GraphQLSchema graphQLSchema = new SchemaGenerator()
                    .makeExecutableSchema(
                            schemaRegistry,
                            buildRuntimeWiring(graphQLMerged, graphQLLinkTypesMap, schemaRegistry)
                    );

            return GraphQL.newGraphQL(graphQLSchema)
                    .instrumentation(new GraphQLBatchInstrumentation(graphQLBatchExecutor, graphQLLinkTypesMap))
                    .build();
        } else {
            return null;
        }
    }

    private RuntimeWiring buildRuntimeWiring(GraphQLMerged merged, GraphQLLinkTypesMap graphQLLinkTypesMap, TypeDefinitionRegistry registry) {
        final RemoteDelegatingDataFetcher remoteDelegatingDataFetcher =
                new RemoteDelegatingDataFetcher(merged, remoteGraphQLExecutionClient, graphQLLinkTypesMap);

        final RemoteDelegatingGraphQLLinkDataFetcher remoteDelegatingGraphQLLinkDataFetcher =
                new RemoteDelegatingGraphQLLinkDataFetcher(graphQLLinkTypesMap, graphQLRemoteLinkExecutor);

        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

        registry.getTypes(UnionTypeDefinition.class).forEach(unionTypeDefinition -> registerResolver(builder, unionTypeDefinition));

        registerScalars(builder, scalarsProviderOptional);

        if (!merged.getOperationKeysByType(GraphQLOperationType.QUERY).isEmpty()) {
            builder.type("Query", typeBuilder -> {
                merged.getOperationKeysByType(GraphQLOperationType.QUERY)
                        .forEach(operationKey ->
                                typeBuilder.dataFetcher(
                                        operationKey.getOperationName(),
                                        remoteDelegatingDataFetcher
                                )
                        );
                return typeBuilder;
            });
        }

        if (!merged.getOperationKeysByType(GraphQLOperationType.MUTATION).isEmpty()) {
            builder.type("Mutation", typeBuilder -> {
                merged.getOperationKeysByType(GraphQLOperationType.MUTATION)
                        .forEach(operationKey ->
                                typeBuilder.dataFetcher(
                                        operationKey.getOperationName(),
                                        remoteDelegatingDataFetcher
                                )
                        );
                return typeBuilder;
            });
        }

        graphQLLinkTypesMap.getAllLinkedFieldCoordinates().forEach(fieldCoordinate -> {
            builder.type(fieldCoordinate.typeName(), typeBuilder -> {
                typeBuilder.dataFetcher(fieldCoordinate.fieldName(), remoteDelegatingGraphQLLinkDataFetcher);
                return typeBuilder;
            });
        });

        return builder.build();
    }

}
