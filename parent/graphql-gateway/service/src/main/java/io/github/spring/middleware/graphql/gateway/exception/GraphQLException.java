package io.github.spring.middleware.graphql.gateway.exception;

import graphql.GraphQLError;
import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.ErrorDescriptor;

import java.util.List;

public class GraphQLException extends RuntimeException implements ErrorDescriptor {

    private GraphQLErrorCodes errorCode;
    private List<GraphQLError> errors;

    public GraphQLException(GraphQLErrorCodes errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public GraphQLException(GraphQLErrorCodes errorCode, String message) {
        this(errorCode, message, (Throwable) null);
    }

    public GraphQLException(GraphQLErrorCodes errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public GraphQLException(GraphQLErrorCodes errorCode, String message, List<GraphQLError> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    @Override
    public ErrorCodes getErrorCode() {
        return errorCode != null ? errorCode : GraphQLErrorCodes.GRAPHQL_UNKNOWN_ERROR;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }
}
