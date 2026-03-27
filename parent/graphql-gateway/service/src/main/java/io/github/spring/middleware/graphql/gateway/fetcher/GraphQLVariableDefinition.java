package io.github.spring.middleware.graphql.gateway.fetcher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GraphQLVariableDefinition {

    private String name;
    private String type;

}
