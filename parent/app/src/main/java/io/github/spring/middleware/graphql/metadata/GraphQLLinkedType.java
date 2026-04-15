package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Data
public class GraphQLLinkedType {

    private String typeName;
    private String parentTypeName;
    private List<String> wrapperTypeNames;
    private Collection<GraphQLFieldLinkDefinition> graphQLFieldLinkDefinitions = new HashSet<>();
}
