package io.github.spring.middleware.graphql.gateway.scalars;

import graphql.schema.GraphQLScalarType;

import java.util.Collection;

public interface ScalarsProvider {

    Collection<GraphQLScalarType> getScalars();

}
