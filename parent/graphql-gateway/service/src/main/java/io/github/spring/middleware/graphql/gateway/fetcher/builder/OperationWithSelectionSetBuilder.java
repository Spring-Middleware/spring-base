package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.Map;

public class OperationWithSelectionSetBuilder extends CommonBuilder {

    public void appendOperationWithSelectionSet(
            Field field,
            DataFetchingEnvironment environment,
            String indent,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName,
            Map<String, Object> queryVariables,
            Map<String, GraphQLVariableDefinition> variableDefinitions,
            boolean stopOnResolvedLink
    ) {
        builder.append(indent).append(field.getName());
        ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder();
        argumentsBuilder.appendArguments(environment.getArguments());
        builder.append(argumentsBuilder.build());

        if (field.getSelectionSet() != null && !field.getSelectionSet().getSelections().isEmpty()) {
            SelectionSetBuilder selectionSetBuilder = new SelectionSetBuilder();
            selectionSetBuilder.appendSelectionSet(field.getSelectionSet(), indent, resolvedLinksByFieldName, queryVariables, variableDefinitions, stopOnResolvedLink);
            builder.append(selectionSetBuilder.build());
        }
    }

}
