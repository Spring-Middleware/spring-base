package io.github.spring.middleware.graphql.controller;

import graphql.ExecutionInput;
import graphql.GraphQL;
import io.github.spring.middleware.graphql.annotations.GraphQLEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


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
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query((String) request.get("query"))
                .variables((Map<String, Object>) request.get("variables"))
                .build();

        return graphQL.execute(input).toSpecification();
    }

    @GetMapping("/_alive")
    public Map<String, Object> alive() {
        return Map.of("status", "UP");
    }
}
