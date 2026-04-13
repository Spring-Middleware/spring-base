package io.github.spring.middleware.graphql.gateway.exception;

import io.github.spring.middleware.error.ErrorCodes;

public enum GraphQLErrorCodes implements ErrorCodes {

    SCHEMA_FETCH_ERROR("SCHEMA_FETCH_ERROR"),
    SCHEMA_PARSE_ERROR("SCHEMA_PARSE_ERROR"),
    SCHEMA_MERGE_ERROR("SCHEMA_MERGE_ERROR"),
    REMOTE_EXECUTION_ERROR("REMOTE_EXECUTION_ERROR"),
    VALUE_NORMALIZATION_ERROR("VALUE_NORMALIZATION_ERROR"),
    SCHEMA_METADATA_FETCH_ERROR("SCHEMA_METADATA_FETCH_ERROR"),
    FIELD_EXTRACTION_ERROR("FIELD_EXTRACTION_ERROR"),
    GRAPHQL_UNKNOWN_ERROR("GRAPHQL_UNKNOWN_ERROR");

    private final String code;

    GraphQLErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
