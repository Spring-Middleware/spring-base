package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.Field;
import graphql.language.SelectionSet;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.builder.QueryBuildContext.extractArguments;

public class NestedFieldBuilder extends CommonBuilder {


    public void appendNestedField(
            Field field,
            String indent,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName,
            Map<String, Object> queryVariables,
            Map<String, GraphQLVariableDefinition> variableDefinitions,
            boolean stopOnResolvedLink
    ) {
        builder.append("\n").append(indent).append(field.getName());

        Map<String, Object> args = extractArguments(field, variableDefinitions);
        queryVariables.putAll(args);

        ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder();
        argumentsBuilder.appendArguments(args);
        builder.append(argumentsBuilder.build());

        GraphQLLinkTypesMap.GraphQLResolvedLink link = resolvedLinksByFieldName.get(field.getName());
        if (stopOnResolvedLink && link != null) {
            return;
        }

        SelectionSet selectionSet = field.getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            SelectionSetBuilder selectionSetBuilder = new SelectionSetBuilder();
            selectionSetBuilder.appendSelectionSet(
                    selectionSet,
                    indent,
                    resolvedLinksByFieldName,
                    queryVariables,
                    variableDefinitions,
                    stopOnResolvedLink
            );
            builder.append(selectionSetBuilder.build());
        }
    }

}
