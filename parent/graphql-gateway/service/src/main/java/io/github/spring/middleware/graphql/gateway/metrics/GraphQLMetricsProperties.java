package io.github.spring.middleware.graphql.gateway.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "middleware.graphql.gateway.metrics")
public class GraphQLMetricsProperties {

    private GraphQLMetricsMode mode = GraphQLMetricsMode.DISABLED;
    private String headerName = "X-Metrics";
    private String headerValue = "true";

    public GraphQLMetricsMode getMode() {
        return mode;
    }

    public void setMode(GraphQLMetricsMode mode) {
        this.mode = mode;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }
}
