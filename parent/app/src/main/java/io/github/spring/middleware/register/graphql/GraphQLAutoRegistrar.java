package io.github.spring.middleware.register.graphql;

import io.github.spring.middleware.graphql.annotations.GraphQLEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class GraphQLAutoRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final GraphQLSchemaRegister graphQLSchemaRegister;

    public GraphQLAutoRegistrar(GraphQLSchemaRegister graphQLSchemaRegister) {
        this.graphQLSchemaRegister = graphQLSchemaRegister;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        // Scan beans annotated with @Register
        Set<Class<?>> resourceClasses = event.getApplicationContext().getBeansWithAnnotation(GraphQLEndpoint.class)
                .values().stream()
                .map(bean -> AopUtils.getTargetClass(bean))
                .collect(Collectors.toSet());

        graphQLSchemaRegister.register(resourceClasses);


    }
}
