package io.github.spring.middleware.graphql.gateway.factory;

import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import io.github.spring.middleware.graphql.gateway.builder.GraphQLSchemaDefinitionBuilder;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.fetcher.RemoteDelegatingDataFetcher;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryLoader;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLTypeRegistryMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLTypeRegistryMerger;
import io.github.spring.middleware.graphql.gateway.scalars.InstantScalar;
import io.github.spring.middleware.graphql.gateway.scalars.OffsetDateTimeScalar;
import io.github.spring.middleware.graphql.gateway.scalars.ScalarsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class GraphQLGatewayFactory {

    private final RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;
    private final GraphQLTypeRegistryMerger typeRegistryMerger;
    private final GraphQLSchemaDefinitionBuilder schemaDefinitionBuilder;
    private final GraphQLTypeRegistryLoader registryLoader;
    private final Optional<ScalarsProvider> scalarsProviderOptional;

    public GraphQL build() {
        final GraphQLTypeRegistryMap graphQLTypeRegistryMap = registryLoader.loadTypeRegistryMap();
        final GraphQLMerged graphQLMerged = typeRegistryMerger.merge(graphQLTypeRegistryMap);
        final GraphQLSchema graphQLSchema = new SchemaGenerator()
                .makeExecutableSchema(
                        schemaDefinitionBuilder.build(graphQLMerged, graphQLTypeRegistryMap),
                        buildRuntimeWiring(graphQLMerged)
                );

        return GraphQL.newGraphQL(graphQLSchema).build();
    }


    private RuntimeWiring buildRuntimeWiring(GraphQLMerged merged) {
        final RemoteDelegatingDataFetcher remoteDelegatingDataFetcher =
                new RemoteDelegatingDataFetcher(merged, remoteGraphQLExecutionClient);

        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

        registerScalars(builder);

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

        return builder.build();
    }

    private void registerScalars(RuntimeWiring.Builder builder) {
        builder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.CountryCode)
                .scalar(ExtendedScalars.Currency)
                .scalar(ExtendedScalars.Locale)
                .scalar(ExtendedScalars.LocalTime)
                .scalar(InstantScalar.INSTANCE)
                .scalar(OffsetDateTimeScalar.INSTANCE);

        scalarsProviderOptional.ifPresent(provider -> provider.getScalars().forEach(builder::scalar));
    }
}
