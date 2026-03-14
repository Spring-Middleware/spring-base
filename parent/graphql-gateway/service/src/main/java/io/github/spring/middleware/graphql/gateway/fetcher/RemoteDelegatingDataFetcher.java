package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLMerged;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationKey;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;
import io.github.spring.middleware.registry.model.SchemaLocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (!(dataObject instanceof Map<?, ?> dataMap)) {
            return null;
        }

        return dataMap.get(fieldName);
    }

    private String buildGraphQLQueryWithVariables(
            DataFetchingEnvironment environment,
            GraphQLOperationType operationType,
            String fieldName
    ) {
        StringBuilder sb = new StringBuilder();
        String operation = operationType == GraphQLOperationType.MUTATION ? "mutation" : "query";

        sb.append(operation).append(" ").append(fieldName);

        String variablesDefinition = buildVariablesDefinition(environment);
        if (!variablesDefinition.isEmpty()) {
            sb.append("(").append(variablesDefinition).append(")");
        }

        sb.append(" {");
        appendRootFieldWithVariables(environment.getField(), environment, sb, "\n  ");
        sb.append("\n}");

        return sb.toString();
    }

    private String buildVariablesDefinition(DataFetchingEnvironment environment) {
        List<GraphQLArgument> arguments = environment.getFieldDefinition().getArguments();
        if (arguments.isEmpty()) {
            return "";
        }

        return arguments.stream()
                .map(argument -> STR."$\{argument.getName()}: \{renderInputType(argument.getType())}")
                .collect(Collectors.joining(", "));
    }

    private String renderInputType(GraphQLInputType type) {
        return type.toString();
    }

    private void appendRootFieldWithVariables(
            Field field,
            DataFetchingEnvironment environment,
            StringBuilder sb,
            String indent
    ) {
        sb.append(indent).append(field.getName());

        Map<String, Object> arguments = environment.getArguments();
        if (!arguments.isEmpty()) {
            sb.append("(");
            String argsAsVariables = arguments.keySet().stream()
                    .map(argumentName -> STR."\{argumentName}: $\{argumentName}")
                    .collect(Collectors.joining(", "));
            sb.append(argsAsVariables).append(")");
        }

        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            sb.append(" {");
            for (Selection<?> selection : selectionSet.getSelections()) {
                if (selection instanceof Field childField) {
                    appendNestedField(childField, sb, STR."\{indent}  ");
                }
            }
            sb.append(indent).append("}");
        }
    }

    private void appendNestedField(Field field, StringBuilder sb, String indent) {
        sb.append("\n").append(indent).append(field.getName());

        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            sb.append(" {");
            for (Selection<?> selection : selectionSet.getSelections()) {
                if (selection instanceof Field childField) {
                    appendNestedField(childField, sb, STR."\{indent}  ");
                }
            }
            sb.append("\n").append(indent).append("}");
        }
    }
}