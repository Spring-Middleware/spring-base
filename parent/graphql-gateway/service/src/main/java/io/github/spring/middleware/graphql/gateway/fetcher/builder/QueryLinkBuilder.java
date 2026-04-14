package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLType;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.builder.QueryBuildContext.renderInputType;

public class QueryLinkBuilder extends CommonBuilder {

    public void appendGraphQLQuery(
            DataFetchingEnvironment environment,
            GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink,
            Map<String, Object> variables,
            Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName
    ) {
        String remoteQuery = resolvedLink.getFieldLinkDefinition().getQuery();
        List<GraphQLArgumentLinkDefinition> arguments =
                resolvedLink.getFieldLinkDefinition().getArgumentLinkDefinitions();

        Map<String, GraphQLVariableDefinition> variableDefinitions = new LinkedHashMap<>();

        List<GraphQLVariableDefinition> baseDefinitions = buildBaseDefinitions(arguments, resolvedLink);

        builder.append("query");

        VariablesDefinitionBuilder variablesDefinitionBuilder = new VariablesDefinitionBuilder();
        variablesDefinitionBuilder.appendVariablesDefinition(baseDefinitions, variableDefinitions, variables);
        builder.append(variablesDefinitionBuilder.build());

        builder.append(" {");

        LinkOperationBuilder linkOperationBuilder = new LinkOperationBuilder();
        linkOperationBuilder.appendLinkOperation(
                environment,
                remoteQuery,
                arguments,
                variables,
                variableDefinitions,
                resolvedLinksByFieldName
        );
        builder.append(linkOperationBuilder.build());

        builder.append("\n}");
    }

    private List<GraphQLVariableDefinition> buildBaseDefinitions(
            List<GraphQLArgumentLinkDefinition> arguments,
            GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink
    ) {
        if (arguments == null || arguments.isEmpty()) {
            return List.of();
        }

        return arguments.stream()
                .map(argument -> {
                    GraphQLType argumentType = resolvedLink.getTargetFieldArgumentTypes().get(argument.getArgumentName());

                    if (argumentType == null) {
                        throw new IllegalStateException(
                                STR."Target argument type not found for argument: \{argument.getArgumentName()}"
                        );
                    }

                    if (!(argumentType instanceof GraphQLInputType inputType)) {
                        throw new IllegalStateException(
                                STR."Target argument type is not a GraphQLInputType for argument: \{argument.getArgumentName()}"
                        );
                    }

                    return new GraphQLVariableDefinition(
                            argument.getArgumentName(),
                            renderInputType(inputType)
                    );
                })
                .toList();
    }
}
