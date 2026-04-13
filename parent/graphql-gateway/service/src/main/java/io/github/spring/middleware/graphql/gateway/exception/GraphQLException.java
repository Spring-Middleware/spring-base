package io.github.spring.middleware.graphql.gateway.exception;

import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.ErrorDescriptor;

public class GraphQLException extends RuntimeException implements ErrorDescriptor {

    private GraphQLErrorCodes errorCode;

    public GraphQLException(GraphQLErrorCodes errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public GraphQLException(GraphQLErrorCodes errorCode, String message) {
        this(errorCode, message, null);
    }

    public GraphQLException(GraphQLErrorCodes errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCodes getCode() {
        return errorCode != null ? errorCode : GraphQLErrorCodes.GRAPHQL_UNKNOWN_ERROR;
    }
}
