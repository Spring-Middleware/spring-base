package io.github.spring.middleware.graphql.gateway.merger;

import lombok.Value;

@Value
public class GraphQLOperationKey {
    String operationName;
    GraphQLOperationType operationType;
}
