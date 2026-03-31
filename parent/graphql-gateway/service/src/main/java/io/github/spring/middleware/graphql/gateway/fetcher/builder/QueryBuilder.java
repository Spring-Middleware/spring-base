package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.merger.GraphQLOperationType;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryBuilder extends CommonBuilder {

    public void appendGraphQLQueryWithVariables(
            DataFetchingEnvironment environment,
            GraphQLOperationType operationType,
            String fieldName,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName,
            Map<String, Object> queryVariables
    ) {
        String operation = operationType == GraphQLOperationType.MUTATION ? "mutation" : "query";
        builder.append(operation).append(" ").append(fieldName);

        Map<String, GraphQLVariableDefinition> variableDefinitions = new LinkedHashMap<>();

        OperationWithSelectionSetBuilder operationWithSelectionSetBuilder = new OperationWithSelectionSetBuilder();
        operationWithSelectionSetBuilder.append(" {");
        operationWithSelectionSetBuilder.appendOperationWithSelectionSet(environment.getField(), environment, "\n  ", resolvedLinksByFieldName, queryVariables, variableDefinitions, true);
        operationWithSelectionSetBuilder.append("\n}");

        VariablesDefinitionBuilder variablesDefinitionBuffer = new VariablesDefinitionBuilder();
        variablesDefinitionBuffer.appendVariablesDefinition(environment, variableDefinitions, queryVariables);

        builder.append(variablesDefinitionBuffer.build()).append(operationWithSelectionSetBuilder.build());
    }

}
