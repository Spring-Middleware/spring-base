package io.github.spring.middleware.graphql.config;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import io.github.spring.middleware.graphql.arguments.GraphQLLinkArguments;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.TypeMapper;
import io.leangen.graphql.generator.mapping.TypeMappingEnvironment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Set;

public class GraphQLLinkArgumentsTypeMapper implements TypeMapper {

    public boolean supports(AnnotatedElement element, AnnotatedType type){
        return GenericTypeReflector.erase(type.getType()).equals(GraphQLLinkArguments.class);
    }

    @Override
    public GraphQLOutputType toGraphQLType(AnnotatedType javaType,
                                           Set<Class<? extends TypeMapper>> mappersToSkip,
                                           TypeMappingEnvironment env) {
        if (!supports(null, javaType)) {
            return null;
        }
        return GraphQLLinkArgumentsScalar.INSTANCE;
    }

    @Override
    public GraphQLInputType toGraphQLInputType(AnnotatedType javaType,
                                               Set<Class<? extends TypeMapper>> mappersToSkip,
                                               TypeMappingEnvironment env) {
        if (!supports(null, javaType)) {
            return null;
        }
        return GraphQLLinkArgumentsScalar.INSTANCE;
    }
}