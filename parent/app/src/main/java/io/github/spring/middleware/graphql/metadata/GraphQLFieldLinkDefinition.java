package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

@Data
public class GraphQLFieldLinkDefinition {

    private String fieldName;
    private String targetTypeName;
    private String schema;
    private String query;
    private String argumentName;
    private boolean collection;
}
