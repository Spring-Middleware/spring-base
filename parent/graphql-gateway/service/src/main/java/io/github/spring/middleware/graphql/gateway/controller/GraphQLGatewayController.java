package io.github.spring.middleware.graphql.gateway.controller;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLGatewayHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graphql")
@RequiredArgsConstructor
public class GraphQLGatewayController {

    private final GraphQLGatewayHolder holder;


    @PostMapping
    public Map<String, Object> execute(@RequestBody Map<String, Object> request) {

        GraphQL graphQL = holder.getRequired();

        Map<String, Object> variables = Optional
                .ofNullable((Map<String, Object>) request.get("variables"))
                .orElse(Collections.emptyMap());

        String operationName = Optional
                .ofNullable((String) request.get("operationName"))
                .orElse(null);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query((String) request.get("query"))
                .operationName(operationName)
                .variables(variables)
                .build();

        ExecutionResult result = graphQL.execute(executionInput);

        return result.toSpecification();
    }
}
