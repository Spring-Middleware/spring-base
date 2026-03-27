package io.github.spring.middleware.graphql.gateway.fetcher.builder;

import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;

import java.util.List;

public class LinkArgumentsBuilder extends CommonBuilder {

    public void appendLinkArguments(List<GraphQLArgumentLinkDefinition> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return;
        }

        builder.append("(");

        for (int i = 0; i < arguments.size(); i++) {
            GraphQLArgumentLinkDefinition argument = arguments.get(i);

            if (i > 0) {
                builder.append(", ");
            }

            builder.append(argument.getArgumentName())
                    .append(": $")
                    .append(argument.getArgumentName());
        }

        builder.append(")");
    }
}
