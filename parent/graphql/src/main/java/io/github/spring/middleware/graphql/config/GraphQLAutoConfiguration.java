package io.github.spring.middleware.graphql.config;

import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.github.spring.middleware.error.ConstraintErrorResolver;
import io.github.spring.middleware.error.ErrorMessageFactory;
import io.github.spring.middleware.graphql.annotations.GraphQLService;
import io.github.spring.middleware.graphql.handler.GraphQLValidationExceptionHandler;
import io.leangen.graphql.GraphQLSchemaGenerator;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@AllArgsConstructor
@ConditionalOnProperty(prefix = "middleware.graphql", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GraphQLAutoConfiguration {

    private final ConstraintErrorResolver constraintErrorResolver;
    private final ErrorMessageFactory errorMessageFactory;

    @Bean
    public GraphQL graphQL(ApplicationContext context) {

        Map<String, Object> services =
                context.getBeansWithAnnotation(GraphQLService.class);

        if (services.isEmpty()) {
            throw new IllegalStateException(
                    "No @GraphQLService beans found. Disable GraphQL autoconfiguration or register at least one service."
            );
        }


        GraphQLSchemaGenerator generator = new GraphQLSchemaGenerator();
        services.values()
                .forEach(generator::withOperationsFromSingleton);
        GraphQLSchema schema = generator.generate();
        return GraphQL.newGraphQL(schema)
                .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphQLValidationExceptionHandler(constraintErrorResolver, errorMessageFactory)))
                .queryExecutionStrategy(new AsyncExecutionStrategy(new GraphQLValidationExceptionHandler(constraintErrorResolver, errorMessageFactory)))
                .build();

    }
}