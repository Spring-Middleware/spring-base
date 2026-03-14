package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.schema.*;

import java.time.OffsetDateTime;

public class OffsetDateTimeScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("OffsetDateTime")
            .description("Java OffsetDateTime scalar")
            .coercing(new Coercing<OffsetDateTime, String>() {

                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof OffsetDateTime odt) {
                        return odt.toString();
                    }
                    throw new CoercingSerializeException(
                            STR."Expected OffsetDateTime but was \{dataFetcherResult.getClass()}"
                    );
                }

                @Override
                public OffsetDateTime parseValue(Object input) {
                    if (input instanceof String value) {
                        return OffsetDateTime.parse(value);
                    }
                    throw new CoercingParseValueException(
                            "Expected ISO-8601 OffsetDateTime string"
                    );
                }

                @Override
                public OffsetDateTime parseLiteral(Object input) {
                    if (input instanceof graphql.language.StringValue stringValue) {
                        return OffsetDateTime.parse(stringValue.getValue());
                    }
                    throw new CoercingParseLiteralException(
                            "Expected StringValue for OffsetDateTime"
                    );
                }
            })
            .build();
}
