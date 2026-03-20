package io.github.spring.middleware.graphql.builder;

import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
public class GraphQLFieldLinkDefinitionBuilder {

    public GraphQLFieldLinkDefinition buildFromField(BeanInfo beanInfo, Field field, GraphQLLink graphQLLink) {
        GraphQLFieldLinkDefinition definition = new GraphQLFieldLinkDefinition();
        final String fieldName = getFieldName(beanInfo, field);
        if (fieldName == null) {
            return null;
        }
        definition.setFieldName(fieldName);
        definition.setSchema(graphQLLink.schema());
        definition.setTargetTypeName(graphQLLink.type());
        definition.setQuery(graphQLLink.query());
        definition.setArgumentName(graphQLLink.argument());
        return definition;
    }

    public GraphQLFieldLinkDefinition buildFromMethod(BeanInfo beanInfo, Method method, GraphQLLink graphQLLink) {
        if (!method.isAnnotationPresent(GraphQLQuery.class)) {
            log.warn("Skipping method {} on bean {}", method.getName(), beanInfo.getBeanDescriptor().getName());
            return null;
        }
        GraphQLQuery graphQLQuery = method.getAnnotation(GraphQLQuery.class);
        GraphQLFieldLinkDefinition definition = new GraphQLFieldLinkDefinition();
        definition.setFieldName(graphQLQuery.name());
        definition.setSchema(graphQLLink.schema());
        definition.setTargetTypeName(graphQLLink.type());
        definition.setQuery(graphQLLink.query());
        definition.setArgumentName(graphQLLink.argument());
        return definition;
    }


    private String getFieldName(BeanInfo beanInfo, Field field) {
        return Stream.of(beanInfo.getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getName().equals(field.getName()))
                .map(propertyDescriptor -> {
                    Method readMethod = propertyDescriptor.getReadMethod();
                    if (readMethod != null && readMethod.isAnnotationPresent(GraphQLQuery.class)) {
                        return readMethod.getAnnotation(GraphQLQuery.class).name();
                    } else {
                        log.warn("Skipping field {} on bean {}", field.getName(), beanInfo.getBeanDescriptor().getName());
                        return null;
                    }
                }).filter(Objects::nonNull).findFirst().orElse(null);
    }

}
