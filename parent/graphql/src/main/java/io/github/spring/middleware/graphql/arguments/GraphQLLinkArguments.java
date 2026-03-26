package io.github.spring.middleware.graphql.arguments;

import java.util.Map;

public final class GraphQLLinkArguments {

    private final Map<String, Object> values;

    public GraphQLLinkArguments(Map<String, Object> values) {
        this.values = values;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
