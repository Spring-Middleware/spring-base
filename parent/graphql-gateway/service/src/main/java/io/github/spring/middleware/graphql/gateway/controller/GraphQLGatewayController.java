package io.github.spring.middleware.graphql.gateway.controller;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLLinkResolvedBatchedRegistry;
import io.github.spring.middleware.graphql.gateway.cache.GraphQLCachingToggle;
import io.github.spring.middleware.graphql.gateway.metrics.GraphQLMetricsModeResolver;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLBatchingToggle;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLGatewayHolder;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLGatewayInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graphql")
@RequiredArgsConstructor
public class GraphQLGatewayController {

    private final GraphQLGatewayHolder holder;
    private final GraphQLBatchingToggle batchingToggle;
    private final GraphQLCachingToggle cachingToggle;
    private final GraphQLGatewayInitializer graphQLGatewayInitializer;
    private final GraphQLMetricsModeResolver graphQLMetricsModeResolver;


    @PostMapping("/batching-toggle")
    public void setBatchingEnabled(@RequestParam("enabled") Boolean enabled) {
        if (enabled != null) {
            batchingToggle.setEnabled(enabled);
        }
    }

    @GetMapping("/batching-toggle")
    public Map<String, Object> getBatchingStatus() {
        return Map.of("enabled", batchingToggle.isEnabled());
    }


    @PostMapping("/caching-toggle")
    public void setCachingToggle(@RequestParam("enabled") Boolean enabled) {
        if (enabled != null) {
            cachingToggle.setEnabled(enabled);
        }
    }

    @GetMapping("/caching-toggle")
    public Map<String, Object> getCachingToggle() {
        return Map.of("enabled", cachingToggle.isEnabled());
    }


    @GetMapping("/refresh")
    public void refresh() {
        graphQLGatewayInitializer.refresh();
    }


    @PostMapping
    public Map<String, Object> execute(@RequestBody Map<String, Object> request, @RequestHeader HttpHeaders headers) {

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
                .graphQLContext(getContext(headers))
                .build();

        ExecutionResult result = graphQL.execute(executionInput);

        return result.toSpecification();
    }

    private Map<String, Object> getContext(HttpHeaders headers) {
        final var context = new HashMap<String, Object>();
        context.put("batchedRegistry", new GraphQLLinkResolvedBatchedRegistry());
        context.put(GraphQLMetricsModeResolver.METRICS_ENABLED_CONTEXT_KEY, graphQLMetricsModeResolver.isEnabled(headers));
        return context;
    }

}
