package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.net.URI;

public class URIScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("URI")
            .description("Java URI scalar")
            .coercing(new Coercing<URI, String>() {

                @Override
                public String serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof URI uri) {
                        return uri.toString();
                    }
                    throw new CoercingSerializeException(
                            STR."Expected URI but was \{dataFetcherResult.getClass()}"
                    );
                }

                @Override
                public URI parseValue(Object input) {
                    if (input instanceof String value) {
                        try {
                            return URI.create(value);
                        } catch (IllegalArgumentException ex) {
                            throw new CoercingParseValueException(
                                    STR."Invalid URI: \{value}", ex
                            );
                        }
                    }
                    throw new CoercingParseValueException(
                            "Expected URI string"
                    );
                }

                @Override
                public URI parseLiteral(Object input) {
                    if (input instanceof graphql.language.StringValue stringValue) {
                        try {
                            return URI.create(stringValue.getValue());
                        } catch (IllegalArgumentException ex) {
                            throw new CoercingParseLiteralException(
                                    STR."Invalid URI: \{stringValue.getValue()}", ex
                            );
                        }
                    }
                    throw new CoercingParseLiteralException(
                            "Expected StringValue for URI"
                    );
                }
            })
            .build();
}