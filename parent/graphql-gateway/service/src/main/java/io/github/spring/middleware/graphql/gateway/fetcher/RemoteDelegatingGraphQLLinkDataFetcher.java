package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.extractor.GraphQLSourceFieldExtractor;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.HashMap;
import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.QueryBuilder.appendSelectionSet;

public class RemoteDelegatingGraphQLLinkDataFetcher implements DataFetcher<Object> {

    private GraphQLLinkTypesMap graphQLLinkTypesMap;
    private GraphQLSourceFieldExtractor graphQLSourceFieldExtractor;

    public RemoteDelegatingGraphQLLinkDataFetcher(GraphQLLinkTypesMap graphQLLinkTypesMap, GraphQLSourceFieldExtractor graphQLSourceFieldExtractor) {
        this.graphQLLinkTypesMap = graphQLLinkTypesMap;
        this.graphQLSourceFieldExtractor = graphQLSourceFieldExtractor;
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
        String argumentName = resolvedLink.fieldLinkDefinition().getArgumentName();
        Object argumentValue = graphQLSourceFieldExtractor.extractFieldValue(source, resolvedLink.fieldLinkDefinition().getFieldName());

        Map<String, Object> variables = new HashMap<>();
        variables.put(argumentName, argumentValue);

        String query = buildGraphQLQuery(environment, resolvedLink);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .graphQLContext(builder -> {
                    GraphQLContext originalContext = environment.getGraphQlContext();
                    builder.of(originalContext);
                }).build();
        return null;

    }


    private String buildGraphQLQuery(DataFetchingEnvironment environment,
                                     GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink) {
        String argumentName = resolvedLink.fieldLinkDefinition().getArgumentName();
        String remoteQuery = resolvedLink.fieldLinkDefinition().getQuery();

        StringBuilder sb = new StringBuilder();
        sb.append("query($").append(argumentName).append(": JSON) {\n");
        sb.append("  ").append(remoteQuery);
        sb.append("(").append(argumentName).append(": $").append(argumentName).append(")");

        SelectionSet selectionSet = environment.getField().getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            appendSelectionSet(selectionSet, sb, "  ");
        }

        sb.append("\n}");
        return sb.toString();
    }

}
