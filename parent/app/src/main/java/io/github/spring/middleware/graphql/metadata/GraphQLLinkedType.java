package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

import java.util.Collection;
import java.util.HashSet;

@Data
public class GraphQLLinkedType {

    private String typeName;
    private Collection<GraphQLFieldLinkDefinition> graphQLFieldLinkDefinitions = new HashSet<>();
}
