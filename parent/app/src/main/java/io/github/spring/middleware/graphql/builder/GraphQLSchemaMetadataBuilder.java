package io.github.spring.middleware.graphql.builder;

import io.github.spring.middleware.annotation.graphql.GraphQLLinkClass;
import io.github.spring.middleware.graphql.metadata.GraphQLSchemaMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLSchemaMetadataBuilder {

    private final GraphQLLinkedTypeBuilder linkedTypeBuilder;

    public GraphQLSchemaMetadata build(List<String> basePackages) {
        final GraphQLSchemaMetadata metadata = new GraphQLSchemaMetadata();
        Reflections reflections = new Reflections(basePackages);
        final Set<Class<?>> linkClasses = reflections.getTypesAnnotatedWith(GraphQLLinkClass.class);
        linkClasses.stream().map(linkClazz -> {
                    try {
                        return linkedTypeBuilder.build(linkClazz);
                    } catch (IntrospectionException e) {
                        log.warn("Skipping class {} due to introspection error: {}", linkClazz.getName(), e.getMessage());
                        return null;
                    }
                }).filter(Objects::nonNull)
                .filter(graphQLLinkedType -> !graphQLLinkedType.getGraphQLFieldLinkDefinitions().isEmpty())
                .forEach(metadata.getGraphQLLinkedTypes()::add);
        return metadata;
    }

}
