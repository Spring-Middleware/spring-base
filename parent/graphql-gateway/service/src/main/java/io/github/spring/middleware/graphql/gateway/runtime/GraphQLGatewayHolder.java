package io.github.spring.middleware.graphql.gateway.runtime;

import graphql.GraphQL;
import org.springframework.stereotype.Component;

@Component
public class GraphQLGatewayHolder {

    private volatile GraphQL graphQL;

    public void initialize(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    public GraphQL getRequired() {
        if (graphQL == null) {
            throw new IllegalStateException("GraphQL Gateway is not initialized yet");
        }
        return graphQL;
    }

    public GraphQL getIfAvailable() {
        return graphQL;
    }

    public boolean isReady() {
        return graphQL != null;
    }

    public void refresh(GraphQL graphQL) {
        this.graphQL = graphQL;
    }
}
