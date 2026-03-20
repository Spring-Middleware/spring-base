package io.github.spring.middleware.graphql.builder;

import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.annotation.graphql.GraphQLType;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLLinkedType;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class GraphQLLinkedTypeBuilder {

    private final GraphQLFieldLinkDefinitionBuilder fieldLinkDefinitionBuilder;

    public GraphQLLinkedType build(Class<?> clazz) throws IntrospectionException {
        GraphQLLinkedType graphQLLinkedType = new GraphQLLinkedType();
        graphQLLinkedType.setTypeName(getTypeName(clazz));
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        Set<Field> fieldsLinked = scanLinkedFields(clazz);
        fieldsLinked.stream().forEach(field -> {
            GraphQLLink graphQLLink = field.getAnnotation(GraphQLLink.class);
            GraphQLFieldLinkDefinition fieldLinkDefinition = fieldLinkDefinitionBuilder.buildFromField(beanInfo, field, graphQLLink);
            if (fieldLinkDefinition != null) {
                graphQLLinkedType.getGraphQLFieldLinkDefinitions().add(fieldLinkDefinition);
            }
        });
        Stream.of(clazz.getMethods())
                .filter(method -> method.isAnnotationPresent(GraphQLLink.class) && method
                        .isAnnotationPresent(GraphQLQuery.class))
                .forEach(method -> {
                    GraphQLLink graphQLLink = method.getAnnotation(GraphQLLink.class);
                    GraphQLFieldLinkDefinition fieldLinkDefinition = fieldLinkDefinitionBuilder.buildFromMethod(beanInfo, method, graphQLLink);
                    if (fieldLinkDefinition != null) {
                        graphQLLinkedType.getGraphQLFieldLinkDefinitions().add(fieldLinkDefinition);
                    }
                });

        return graphQLLinkedType;
    }


    private Set<Field> scanLinkedFields(Class<?> clazz) {
        Set<Field> fieldsLinked = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(GraphQLLink.class)) {
                fieldsLinked.add(field);
            }
        }
        if (clazz.getSuperclass() != null) {
            fieldsLinked.addAll(scanLinkedFields(clazz.getSuperclass()));
        }
        return fieldsLinked;
    }

    private String getTypeName(Class<?> clazz) {
        return Optional.ofNullable(
                        clazz.getAnnotation(GraphQLType.class)
                ).map(GraphQLType::name)
                .filter(name -> !name.trim().isEmpty())
                .orElse(clazz.getSimpleName());
    }
}




