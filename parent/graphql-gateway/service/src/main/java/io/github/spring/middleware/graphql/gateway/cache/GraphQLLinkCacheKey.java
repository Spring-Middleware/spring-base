package io.github.spring.middleware.graphql.gateway.cache;

import java.util.Map;
import java.util.Objects;

public record GraphQLLinkCacheKey(String schema, String query, Map<String, Object> variables) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphQLLinkCacheKey that = (GraphQLLinkCacheKey) o;
        return Objects.equals(query, that.query) && Objects.equals(schema, that.schema) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, query, variables);
    }
}
