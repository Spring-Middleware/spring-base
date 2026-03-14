package io.github.spring.middleware.graphql.gateway.merger;

import graphql.language.FieldDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLOperation {

    private GraphQLOperationType type;
    private String operationName;
    private FieldDefinition fieldDefinition;


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphQLOperation that = (GraphQLOperation) o;
        return type == that.type && Objects.equals(operationName, that.operationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, operationName);
    }

    public GraphQLOperationKey getKey() {
        return new GraphQLOperationKey(operationName, type);
    }
}
