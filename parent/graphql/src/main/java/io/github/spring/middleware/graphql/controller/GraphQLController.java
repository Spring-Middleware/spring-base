package io.github.spring.middleware.graphql.controller;

import graphql.ExecutionInput;
import graphql.GraphQL;
import io.github.spring.middleware.graphql.annotations.GraphQLEndpoint;
import jakarta.annotation.security.PermitAll;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;


@GraphQLEndpoint
@RestController
@RequestMapping("/graphql")
@ConditionalOnBean(GraphQL.class)
public class GraphQLController {

    private final GraphQL graphQL;

    public GraphQLController(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    @PostMapping
    public Map<String, Object> execute(@RequestBody Map<String, Object> request) {


        Map<String, Object> variables = Optional
                .ofNullable((Map<String, Object>) request.get("variables"))
                .orElse(Collections.emptyMap());

        String operationName = Optional
                .ofNullable((String) request.get("operationName"))
                .orElse(null);

        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query((String) request.get("query"))
                .operationName(operationName)
                .variables(variables)
                .build();

        return graphQL.execute(input).toSpecification();
    }

    @PermitAll
    @GetMapping("/_alive")
    public Map<String, Object> alive() {
        return Map.of("status", "UP");
    }
}
