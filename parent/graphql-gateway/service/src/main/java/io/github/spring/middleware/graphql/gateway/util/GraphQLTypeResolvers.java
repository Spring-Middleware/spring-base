package io.github.spring.middleware.graphql.gateway.util;

import graphql.TypeResolutionEnvironment;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphQLTypeResolvers {

    public static TypeResolver fromUnionDefinition(UnionTypeDefinition unionTypeDefinition) {
        Set<String> allowedTypeNames = unionTypeDefinition.getMemberTypes().stream()
                .filter(TypeName.class::isInstance)
                .map(TypeName.class::cast)
                .map(TypeName::getName)
                .collect(Collectors.toSet());

        return new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                Map value = env.getObject();
                if (value == null) {
                    return null;
                }

                String candidateTypeName = (String)value.get("__typename");

                if (!allowedTypeNames.contains(candidateTypeName)) {
                    return null;
                }
                return env.getSchema().getObjectType(candidateTypeName);
            }
        };
    }

}
