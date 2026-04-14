package io.github.spring.middleware.graphql.builder;

import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.annotation.graphql.GraphQLLinkClass;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class GraphQLLinkedTypeBuilder {

    private final GraphQLFieldLinkDefinitionBuilder fieldLinkDefinitionBuilder;

    public GraphQLLinkedType build(Class<?> clazz) throws IntrospectionException {
        GraphQLLinkedType graphQLLinkedType = new GraphQLLinkedType();
        List<String> typeNames = getTypesName(clazz, false);
        if (typeNames.size() > 1) {
            throw new IllegalArgumentException(STR."Multiple type names defined for class \{clazz.getName()}");
        }
        graphQLLinkedType.setTypeName(typeNames.get(0));
        graphQLLinkedType.setWrapperTypeNames(getTypesName(clazz, true));
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

    private List<String> getTypesName(Class<?> clazz, boolean isWrapper) {
        Set<String> names = new LinkedHashSet<>();

        if (!isWrapper) {
            names.add(clazz.getSimpleName());
        }

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            GraphQLLinkClass linkClass = current.getDeclaredAnnotation(GraphQLLinkClass.class);

            if (linkClass != null && linkClass.types().length > 0) {
                Class<?> finalCurrent = current;
                Arrays.stream(linkClass.types())
                        .filter(graphQLType -> graphQLType.isWrapper() == isWrapper)
                        .map(GraphQLType::names)
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .filter(name -> isWrapper || finalCurrent == clazz)
                        .forEach(names::add);
            }

            current = current.getSuperclass();
        }

        return List.copyOf(names);
    }
}




