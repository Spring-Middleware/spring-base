package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.InlineFragment;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.Map;

public class InLineFragmentBuilder extends CommonBuilder {

    public void appendInlineFragment(InlineFragment inlineFragment, String indent, Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName, Map<String, Object> queryVariables, Map<String, GraphQLVariableDefinition> variableDefinitions, boolean stopOnResolvedLink) {
        builder.append("\n").append(indent).append("... on ").append(inlineFragment.getTypeCondition().getName());
        SelectionSetBuilder selectionSetBuilder = new SelectionSetBuilder();
        selectionSetBuilder.appendSelectionSet(inlineFragment.getSelectionSet(), STR."\{indent}  ", resolvedLinksByFieldName, queryVariables, variableDefinitions, stopOnResolvedLink);
        builder.append(selectionSetBuilder.build());
    }

}
