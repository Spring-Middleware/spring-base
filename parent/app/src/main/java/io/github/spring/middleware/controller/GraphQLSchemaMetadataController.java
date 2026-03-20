package io.github.spring.middleware.controller;

import io.github.spring.middleware.graphql.config.GraphQLSchemaMetadataHolder;
import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GraphQLSchemaMetadataController {


    private final GraphQLSchemaMetadataHolder graphQLSchemaMetadataHolder;

    @GetMapping("/graphql/schema-metadata")
    public GraphQLSchemaMetadata getSchemaMetadata() {
        return this.graphQLSchemaMetadataHolder.getIfAvailable();
    }

}
