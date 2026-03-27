package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.fetcher.builder.QueryBuilder;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationKey;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.mapErrors;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.normalizeValue;

public class RemoteDelegatingDataFetcher implements DataFetcher<Object> {

    private final GraphQLMerged graphQLMerged;
    private final RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;
    private final GraphQLLinkTypesMap graphQLLinkTypesMap;

    public RemoteDelegatingDataFetcher(
            GraphQLMerged graphQLMerged,
            RemoteGraphQLExecutionClient remoteGraphQLExecutionClient,
            GraphQLLinkTypesMap graphQLLinkTypesMap
    ) {
        this.graphQLMerged = graphQLMerged;
        this.remoteGraphQLExecutionClient = remoteGraphQLExecutionClient;
        this.graphQLLinkTypesMap = graphQLLinkTypesMap;
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
        List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks = graphQLLinkTypesMap.findGraphQLResolvedLinksForSchemaAndTypeName(schemaLocation, getReturnedTypeName(environment));
        Map<String,GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName = graphQLResolvedLinks.stream().collect(Collectors.toMap(GraphQLLinkTypesMap.GraphQLResolvedLink::getFieldName, link -> link));

        Map<String, Object> variables = environment.getArguments();
        Map<String, Object> queryVariables = new LinkedHashMap<>(variables);

        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.appendGraphQLQueryWithVariables(environment, operationType, fieldName, resolvedLinksByFieldName, queryVariables);
        String query = queryBuilder.build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(fieldName)
                .variables(queryVariables)
                .graphQLContext(builder -> {
                    GraphQLContext originalContext = environment.getGraphQlContext();
                    builder.of(originalContext);
                }).build();

        Map<String, Object> response = remoteGraphQLExecutionClient.execute(schemaLocation, executionInput);
        if (response == null || response.isEmpty()) {
            return null;
        }

        Map<?, ?> dataMap = response.get("data") instanceof Map<?, ?> m ? m : null;
        Object fieldData = dataMap != null ? dataMap.get(environment.getField().getName()) : null;

        Object normalizedData = normalizeValue(fieldData, environment, graphQLResolvedLinks);

        return DataFetcherResult.newResult()
                .data(normalizedData)
                .errors(mapErrors(response.get("errors"), environment))
                .build();
    }


    private String getReturnedTypeName(DataFetchingEnvironment environment) {
        GraphQLType fieldType = environment.getFieldDefinition().getType();
        GraphQLType unwrappedType = GraphQLTypeUtil.unwrapAll(fieldType);

        if (!(unwrappedType instanceof GraphQLNamedType namedType)) {
            throw new GraphQLException(
                    GraphQLErrorCodes.REMOTE_EXECUTION_ERROR,
                    STR."Unsupported return type for remote execution: \{fieldType.getClass()}"
            );
        }
        String typeName = namedType.getName();
        return typeName;
    }

}