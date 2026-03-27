package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.Map;

public class SelectionSetBuilder extends CommonBuilder {

    public void appendSelectionSet(
            SelectionSet selectionSet,
            String indent,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName,
            Map<String, Object> queryVariables,
            Map<String, GraphQLVariableDefinition> variableDefinitions,
            boolean stopOnResolvedLink
    ) {
        builder.append(" {");
        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field childField) {
                NestedFieldBuilder nestedFieldBuilder = new NestedFieldBuilder();
                nestedFieldBuilder.appendNestedField(
                        childField,
                        STR."\{indent}  ",
                        resolvedLinksByFieldName,
                        queryVariables,
                        variableDefinitions,
                        stopOnResolvedLink
                );
                builder.append(nestedFieldBuilder.build());
            } else if (selection instanceof InlineFragment inlineFragment) {
                InLineFragmentBuilder inLineFragmentBuilder = new InLineFragmentBuilder();
                inLineFragmentBuilder.appendInlineFragment(
                        inlineFragment,
                        indent,
                        resolvedLinksByFieldName,
                        queryVariables,
                        variableDefinitions,
                        stopOnResolvedLink
                );
                builder.append(inLineFragmentBuilder.build());
            } else if (selection instanceof FragmentSpread) {
                throw new GraphQLException(
                        GraphQLErrorCodes.SCHEMA_FETCH_ERROR,
                        "FragmentSpread is not supported in this implementation"
                );
            }
        }
        builder.append("\n").append(indent).append("}");
    }

}
