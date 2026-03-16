package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationKey;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.QueryBuilder.buildGraphQLQueryWithVariables;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.mapErrors;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.normalizeValue;

public class RemoteDelegatingDataFetcher implements DataFetcher<Object> {

    private final GraphQLMerged graphQLMerged;
    private final RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;

    public RemoteDelegatingDataFetcher(
            GraphQLMerged graphQLMerged,
            RemoteGraphQLExecutionClient remoteGraphQLExecutionClient
    ) {
        this.graphQLMerged = graphQLMerged;
        this.remoteGraphQLExecutionClient = remoteGraphQLExecutionClient;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        GraphQLObjectType parentType = (GraphQLObjectType) environment.getParentType();
        String parentTypeName = parentType.getName();

        GraphQLOperationType operationType =
                "Mutation".equals(parentTypeName)
                        ? GraphQLOperationType.MUTATION
                        : GraphQLOperationType.QUERY;

        String fieldName = environment.getFieldDefinition().getName();
        GraphQLOperationKey key = new GraphQLOperationKey(fieldName, operationType);

        SchemaLocation schemaLocation = graphQLMerged.getOperationKeySchemaLocationMap().get(key);
        if (schemaLocation == null) {
            throw new GraphQLException(
                    GraphQLErrorCodes.REMOTE_EXECUTION_ERROR,
                    STR."No SchemaLocation found for operation key: \{key}"
            );
        }

        String query = buildGraphQLQueryWithVariables(environment, operationType, fieldName);
        Map<String, Object> variables = environment.getArguments();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(fieldName)
                .variables(variables)
                .graphQLContext(builder -> {
                    GraphQLContext originalContext = environment.getGraphQlContext();
                    builder.of(originalContext);
                }).build();

        Map<String, Object> response = remoteGraphQLExecutionClient.execute(schemaLocation, executionInput);
        if (response == null || response.isEmpty()) {
            return null;
        }

        Object dataObject = response.get("data");
        Object errorsObject = response.get("errors");

        Map<?, ?> dataMap = response.get("data") instanceof Map<?, ?> m ? m : null;
        Object fieldData = dataMap != null ? dataMap.get(environment.getField().getName()) : null;

        Object normalizedData = fieldData instanceof Map<?, ?> map
                ? normalizeValue(map, environment.getFieldType(), environment.getGraphQLSchema())
                : fieldData;

        return DataFetcherResult.newResult()
                .data(normalizedData)
                .errors(mapErrors(response.get("errors"), environment))
                .build();
    }

}