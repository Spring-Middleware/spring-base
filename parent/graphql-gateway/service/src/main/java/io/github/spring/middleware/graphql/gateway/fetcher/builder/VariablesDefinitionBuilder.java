package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLVariableDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.spring.middleware.graphql.gateway.fetcher.builder.QueryBuildContext.buildVariablesDefinition;

public class VariablesDefinitionBuilder extends CommonBuilder {

    public void appendVariablesDefinition(DataFetchingEnvironment environment, Map<String, GraphQLVariableDefinition> variableDefinitions) {
        String variablesDefinition = buildVariablesDefinition(environment, variableDefinitions);
        if (!variablesDefinition.isEmpty()) {
            builder.append("(").append(variablesDefinition).append(")");
        }
    }

    public void appendVariablesDefinition(
            List<GraphQLVariableDefinition> baseDefinitions,
            Map<String, GraphQLVariableDefinition> variableDefinitions
    ) {
        List<String> definitions = new ArrayList<>();

        if (baseDefinitions != null && !baseDefinitions.isEmpty()) {
            for (GraphQLVariableDefinition argument : baseDefinitions) {
                definitions.add(STR."$\{argument.getName()}: \{argument.getType()}");
            }
        }

        if (variableDefinitions != null && !variableDefinitions.isEmpty()) {
            for (GraphQLVariableDefinition var : variableDefinitions.values()) {
                boolean alreadyExists = baseDefinitions != null && baseDefinitions.stream()
                        .anyMatch(arg -> arg.getName().equals(var.getName()));

                if (!alreadyExists) {
                    definitions.add(STR."$\{var.getName()}: \{var.getType()}");
                }
            }
        }

        if (!definitions.isEmpty()) {
            builder.append("(").append(String.join(", ", definitions)).append(")");
        }
    }


}
