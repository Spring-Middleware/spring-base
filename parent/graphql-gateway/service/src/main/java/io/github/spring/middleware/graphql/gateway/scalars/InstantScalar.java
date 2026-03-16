package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;

public class InstantScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("Instant")
            .description("Java Instant scalar")
            .coercing(new Coercing<Instant, String>() {

                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof Instant instant) {
                        return instant.toString();
                    }
                    throw new CoercingSerializeException(
                            STR."Expected Instant but was \{dataFetcherResult.getClass()}"
                    );
                }

                @Override
                public Instant parseValue(Object input) {
                    if (input instanceof String value) {
                        return Instant.parse(value);
                    }
                    throw new CoercingParseValueException(
                            "Expected ISO-8601 Instant string"
                    );
                }

                @Override
                public Instant parseLiteral(Object input) {
                    if (input instanceof graphql.language.StringValue stringValue) {
                        return Instant.parse(stringValue.getValue());
                    }
                    throw new CoercingParseLiteralException(
                            "Expected StringValue for Instant"
                    );
                }
            })
            .build();
}