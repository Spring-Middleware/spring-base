package io.github.spring.middleware.graphql.gateway.exception;

import io.github.spring.middleware.error.ErrorDescriptor;

public enum GraphQLErrorCodes implements ErrorDescriptor {

    SCHEMA_FETCH_ERROR("SCHEMA_FETCH_ERROR"),
    SCHEMA_PARSE_ERROR("SCHEMA_PARSE_ERROR"),
    SCHEMA_MERGE_ERROR("SCHEMA_MERGE_ERROR"),
    REMOTE_EXECUTION_ERROR("REMOTE_EXECUTION_ERROR"),
    VALUE_NORMALIZATION_ERROR("VALUE_NORMALIZATION_ERROR");

    private final String code;

    GraphQLErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
