package io.github.spring.middleware.graphql.builder;

import io.github.spring.middleware.annotation.graphql.GraphQLLink;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
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
        definition.setArgumentLinkDefinitions(buildArgumentLinkDefinitions(graphQLLink));
        definition.setCollection(graphQLLink.collection());
        definition.setBatched(graphQLLink.batched());
        if (isValidGraphQLFieldLinkDefinition(definition, graphQLLink.collection())) {
            return definition;
        } else {
            throw new IllegalArgumentException(STR."Invalid GraphQLFieldLinkDefinition for field \{field.getName()} on bean \{beanInfo.getBeanDescriptor().getName()}");
        }
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
        definition.setArgumentLinkDefinitions(buildArgumentLinkDefinitions(graphQLLink));
        definition.setCollection(graphQLLink.collection());
        definition.setBatched(graphQLLink.batched());
        if (isValidGraphQLFieldLinkDefinition(definition, graphQLLink.collection())) {
            return definition;
        } else {
            throw new IllegalArgumentException(STR."Invalid GraphQLFieldLinkDefinition for method \{method.getName()} on bean \{beanInfo.getBeanDescriptor().getName()}");
        }
    }

    private List<GraphQLArgumentLinkDefinition> buildArgumentLinkDefinitions(GraphQLLink graphQLLink) {
        return Arrays.stream(graphQLLink.arguments()).map(graphQLLinkArgument -> {
            GraphQLArgumentLinkDefinition argumentLinkDefinition = new GraphQLArgumentLinkDefinition();
            argumentLinkDefinition.setArgumentName(graphQLLinkArgument.name());
            argumentLinkDefinition.setTargetTypeName(graphQLLinkArgument.type());
            argumentLinkDefinition.setBatched(graphQLLinkArgument.batch());
            if (!graphQLLinkArgument.targetFieldName().isBlank()) {
                argumentLinkDefinition.setTargetFieldName(graphQLLinkArgument.targetFieldName());
            }
            return argumentLinkDefinition;
        }).toList();
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

    private boolean isValidGraphQLFieldLinkDefinition(GraphQLFieldLinkDefinition graphQLFieldLinkDefinition, boolean isCollection) {
        return !graphQLFieldLinkDefinition.isBatched() || isBatchedValid(graphQLFieldLinkDefinition, isCollection);
    }

    private boolean isBatchedValid(GraphQLFieldLinkDefinition graphQLFieldLinkDefinition, boolean isCollection) {
        return graphQLFieldLinkDefinition.isBatched() &&
                graphQLFieldLinkDefinition.getArgumentLinkDefinitions().stream().anyMatch(GraphQLArgumentLinkDefinition::isBatched) &&
                graphQLFieldLinkDefinition.getArgumentLinkDefinitions().stream().filter(arg -> arg.isBatched())
                        .allMatch(arg -> arg.getTargetFieldName() != null && !arg.getTargetFieldName().isBlank())
                && isCollection;
    }

}
