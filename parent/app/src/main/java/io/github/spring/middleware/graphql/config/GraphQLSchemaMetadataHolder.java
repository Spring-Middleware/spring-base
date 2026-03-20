package io.github.spring.middleware.graphql.config;

import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import org.springframework.stereotype.Component;

@Component
public class GraphQLSchemaMetadataHolder {

    private volatile GraphQLSchemaMetadata graphQLSchemaMetadata;

    public void initialize(GraphQLSchemaMetadata graphQLSchemaMetadata) {
        this.graphQLSchemaMetadata = graphQLSchemaMetadata;
    }

    public GraphQLSchemaMetadata getRequired() {
        if (graphQLSchemaMetadata == null) {
            throw new IllegalStateException("GraphQLSchemaMetadata Gateway is not initialized yet");
        }
        return graphQLSchemaMetadata;
    }

    public GraphQLSchemaMetadata getIfAvailable() {
        return graphQLSchemaMetadata;
    }

    public boolean isReady() {
        return graphQLSchemaMetadata != null;
    }

    public void refresh(GraphQLSchemaMetadata graphQLSchemaMetadata) {
        this.graphQLSchemaMetadata = graphQLSchemaMetadata;
    }

}
