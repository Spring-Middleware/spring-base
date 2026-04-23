package io.github.spring.middleware.graphql.gateway.metrics;

import graphql.GraphQLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class GraphQLMetricsModeResolver {

    public static final String METRICS_ENABLED_CONTEXT_KEY = "graphqlMetricsEnabled";

    private final GraphQLMetricsProperties properties;

    public GraphQLMetricsModeResolver(GraphQLMetricsProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled(HttpHeaders headers) {
        return switch (properties.getMode()) {
            case DISABLED -> false;
            case ENABLED -> true;
            case HEADER -> {
                String headerValue = headers.getFirst(properties.getHeaderName());
                yield properties.getHeaderValue().equalsIgnoreCase(headerValue);
            }
        };
    }

    public boolean isEnabled(GraphQLContext context) {
        Object value = context.get(METRICS_ENABLED_CONTEXT_KEY);
        return value instanceof Boolean enabled && enabled;
    }
}
