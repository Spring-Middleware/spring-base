package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;


import java.util.List;
import java.util.Map;

public class LinkOperationBuilder extends CommonBuilder {

    public void appendLinkOperation(
            DataFetchingEnvironment environment,
            String remoteQuery,
            List<GraphQLArgumentLinkDefinition> arguments,
            Map<String, Object> variables,
            Map<String, GraphQLVariableDefinition> variableDefinitions,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName
    ) {
        builder.append("\n  ").append(remoteQuery);

        LinkArgumentsBuilder linkArgumentsBuilder = new LinkArgumentsBuilder();
        linkArgumentsBuilder.appendLinkArguments(arguments);
        builder.append(linkArgumentsBuilder.build());

        SelectionSet selectionSet = environment.getField().getSelectionSet();
        if (selectionSet != null && !selectionSet.getSelections().isEmpty()) {
            SelectionSetBuilder selectionSetBuilder = new SelectionSetBuilder();
            selectionSetBuilder.appendSelectionSet(
                    selectionSet,
                    "  ",
                    resolvedLinksByFieldName,
                    variables,
                    variableDefinitions,
                    true
            );
            builder.append(selectionSetBuilder.build());
        }
    }
}
