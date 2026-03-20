package io.github.spring.middleware.graphql.config;

import io.github.spring.middleware.graphql.builder.GraphQLSchemaMetadataBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;


@RequiredArgsConstructor
public class GraphQLLinksConfiguration {

    private final GraphQLSchemaMetadataBuilder graphQLSchemaMetadataBuilder;
    private final GraphQLLinksProperties graphQLLinksProperties;
    private final GraphQLSchemaMetadataHolder graphQLSchemaMetadataHolder;

    @PostConstruct
    public void graphQLSchemaMetadata() {
        graphQLSchemaMetadataHolder.initialize(graphQLSchemaMetadataBuilder.build(
                Arrays.asList(graphQLLinksProperties.getBasePackages())
        ));
    }
}
