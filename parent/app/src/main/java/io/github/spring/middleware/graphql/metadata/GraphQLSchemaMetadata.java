package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

import java.util.Collection;
import java.util.HashSet;

@Data
public class GraphQLSchemaMetadata {

    private Collection<GraphQLLinkedType> graphQLLinkedTypes = new HashSet<>();
}
