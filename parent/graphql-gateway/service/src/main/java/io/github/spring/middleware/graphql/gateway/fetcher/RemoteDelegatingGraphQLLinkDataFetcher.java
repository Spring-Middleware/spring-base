package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.QueryBuilder.appendSelectionSet;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLSourceFieldExtractor.extractFieldValue;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.mapErrors;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.normalizeValue;

public class RemoteDelegatingGraphQLLinkDataFetcher implements DataFetcher<Object> {

    private GraphQLLinkTypesMap graphQLLinkTypesMap;
    private RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;

    public RemoteDelegatingGraphQLLinkDataFetcher(GraphQLLinkTypesMap graphQLLinkTypesMap, RemoteGraphQLExecutionClient remoteGraphQLExecutionClient) {
        this.graphQLLinkTypesMap = graphQLLinkTypesMap;
        this.remoteGraphQLExecutionClient = remoteGraphQLExecutionClient;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        String typeName = environment.getExecutionStepInfo().getObjectType().getName();
        String fieldName = environment.getFieldDefinition().getName();

        GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink = graphQLLinkTypesMap.findGraphQLResolvedLink(typeName, fieldName);
        if (resolvedLink == null) {
            throw new IllegalStateException(
                    STR."No resolved link found for type: \{typeName}, field: \{fieldName}"
            );
        }

        Object source = environment.getSource();
        Object extractedValue = extractFieldValue(source, resolvedLink.getFieldLinkDefinition().getFieldName());

        Map<String, Object> variables = new HashMap<>();

        List<GraphQLArgumentLinkDefinition> args =
                resolvedLink.getFieldLinkDefinition().getArgumentLinkDefinitions();

        if (args.size() == 1 && !(extractedValue instanceof Map)) {
            // caso simple (ids, etc.)
            variables.put(args.get(0).getArgumentName(), extractedValue);
        } else {
            // caso múltiple (Map)
            if (!(extractedValue instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        STR."Expected Map for multiple arguments but got: \{extractedValue}"
                );
            }

            for (GraphQLArgumentLinkDefinition argDef : args) {
                String argName = argDef.getArgumentName();
                Object value = map.get(argName);
                if (value == null) {
                    throw new IllegalStateException(
                            STR."Missing argument value for: \{argName}"
                    );
                }
                variables.put(argName, value);
            }
        }

        String query = buildGraphQLQuery(environment, resolvedLink);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .graphQLContext(builder -> {
                    GraphQLContext originalContext = environment.getGraphQlContext();
                    builder.of(originalContext);
                }).build();

        Map<String, Object> response = remoteGraphQLExecutionClient.execute(resolvedLink.getTargetSchemaLocation(), executionInput);
        if (response == null || response.isEmpty()) {
            return null;
        }

        Map<?, ?> dataMap = response.get("data") instanceof Map<?, ?> m ? m : null;
        Object fieldData = dataMap != null ? dataMap.get(resolvedLink.getFieldLinkDefinition().getQuery()) : null;

        Object normalizedData = normalizeValue(fieldData, environment, List.of());

        return DataFetcherResult.newResult()
                .data(normalizedData)
                .errors(mapErrors(response.get("errors"), environment))
                .build();

    }


    private String buildGraphQLQuery(
            DataFetchingEnvironment environment,
            GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink
    ) {
        String remoteQuery = resolvedLink.getFieldLinkDefinition().getQuery();
        List<GraphQLArgumentLinkDefinition> arguments =
                resolvedLink.getFieldLinkDefinition().getArgumentLinkDefinitions();

        StringBuilder sb = new StringBuilder();

        sb.append("query");

        if (arguments != null && !arguments.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                GraphQLArgumentLinkDefinition argument = arguments.get(i);
                GraphQLType argumentType = resolvedLink.getTargetFieldArgumentTypes().get(argument.getArgumentName());

                if (argumentType == null) {
                    throw new IllegalStateException(
                            STR."Target argument type not found for argument: \{argument.getArgumentName()}"
                    );
                }

                if (i > 0) {
                    sb.append(", ");
                }

                sb.append("$")
                        .append(argument.getArgumentName())
                        .append(": ")
                        .append(argumentType);
            }
            sb.append(")");
        }

        sb.append(" {\n");
        sb.append("  ").append(remoteQuery);

        if (arguments != null && !arguments.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                GraphQLArgumentLinkDefinition argument = arguments.get(i);

                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(argument.getArgumentName())
                        .append(": $")
                        .append(argument.getArgumentName());
            }
            sb.append(")");
        }

        SelectionSet selectionSet = environment.getField().getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            appendSelectionSet(selectionSet, sb, "  ", Map.of());
        }

        sb.append("\n}");
        return sb.toString();
    }


}
