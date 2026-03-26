package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

@Data
public class GraphQLArgumentLinkDefinition {

    private String argumentName;
    private String targetTypeName;
}
