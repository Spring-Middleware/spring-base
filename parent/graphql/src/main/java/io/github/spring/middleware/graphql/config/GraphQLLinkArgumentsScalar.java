package io.github.spring.middleware.graphql.config;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import io.github.spring.middleware.graphql.arguments.GraphQLLinkArguments;

public class GraphQLLinkArgumentsScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("GraphQLLinkArguments")
            .description("Internal scalar for GraphQL link arguments")
            .coercing(new Coercing<GraphQLLinkArguments, Object>() {
                @Override
                public Object serialize(Object dataFetcherResult) {
                    if (dataFetcherResult instanceof GraphQLLinkArguments args) {
                        return args.getValues();
                    }
                    return dataFetcherResult;
                }

                @Override
                public GraphQLLinkArguments parseValue(Object input) {
                    if (input instanceof GraphQLLinkArguments args) {
                        return args;
                    }
                    throw new CoercingParseValueException("Expected GraphQLLinkArguments");
                }

                @Override
                public GraphQLLinkArguments parseLiteral(Object input) {
                    throw new CoercingParseLiteralException("Literal parsing not supported");
                }
            })
            .build();
}
