package io.github.spring.middleware.graphql.metadata;

import lombok.Data;

import java.util.List;

@Data
public class GraphQLFieldLinkDefinition {

    private String fieldName;
    private String targetTypeName;
    private String schema;
    private String query;
    private boolean batched;
    private List<GraphQLArgumentLinkDefinition> argumentLinkDefinitions;
    private boolean collection;
}
