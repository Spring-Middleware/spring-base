package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.util.UUID;

public class UUIDScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("UUID")
            .description("Java UUID scalar")
            .coercing(new Coercing<UUID, String>() {

                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof UUID uuid) {
                        return uuid.toString();
                    }
                    if (dataFetcherResult instanceof String value) {
                        return UUID.fromString(value).toString();
                    }
                    throw new CoercingSerializeException(
                            "Expected UUID but was " + dataFetcherResult.getClass()
                    );
                }

                @Override
                public UUID parseValue(Object input) {
                    if (input instanceof String value) {
                        return UUID.fromString(value);
                    }
                    throw new CoercingParseValueException(
                            "Expected UUID string"
                    );
                }

                @Override
                public UUID parseLiteral(Object input) {
                    if (input instanceof StringValue stringValue) {
                        return UUID.fromString(stringValue.getValue());
                    }
                    throw new CoercingParseLiteralException(
                            "Expected StringValue for UUID"
                    );
                }
            })
            .build();
}
