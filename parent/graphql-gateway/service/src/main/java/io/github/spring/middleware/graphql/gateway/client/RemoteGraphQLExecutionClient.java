package io.github.spring.middleware.graphql.gateway.client;

import graphql.ExecutionInput;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.filter.Context;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.metrics.GraphQLGatewayMetrics;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.util.WebClientUtils;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.spring.middleware.config.PropertyNames.REQUEST_HEADERS;
import static io.github.spring.middleware.utils.EndpointUtils.joinUrl;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeContextPath;
import static io.github.spring.middleware.utils.EndpointUtils.normalizeEndpoint;
import static io.github.spring.middleware.utils.EndpointUtils.normalizePath;

@Component
@RequiredArgsConstructor
public class RemoteGraphQLExecutionClient {

    private static final String REQUEST_ID = "REQUEST-ID";

    private final WebClient webClient = WebClientUtils.createWebClient(3000, 10);
    private final GraphQLGatewayMetrics metrics;


    public Map<String, Object> execute(SchemaLocation schemaLocation, ExecutionInput executionInput) {
        final String endpoint = buildGraphQLEndpoint(schemaLocation);
        Timer.Sample sample = null;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", executionInput.getQuery());
            requestBody.put("operationName", executionInput.getOperationName());
            requestBody.put("variables", Optional.ofNullable(executionInput.getVariables()).orElse(Map.of()));

            sample = metrics.startRemoteExecutionTimer(executionInput.getGraphQLContext());

            Map<String, Object> response = webClient.post()
                    .uri("http://" + endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> copyHeaders(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            metrics.recordRemoteExecution(executionInput.getGraphQLContext(), schemaLocation.getNamespace(), executionInput.getOperationName(), "success", sample);

            return response != null ? response : Map.of();
        } catch (Exception e) {
            metrics.recordRemoteExecution(executionInput.getGraphQLContext(), schemaLocation.getNamespace(), executionInput.getOperationName(), "error", sample);
            throw new GraphQLException(
                    GraphQLErrorCodes.REMOTE_EXECUTION_ERROR,
                    STR."Error executing remote GraphQL request against endpoint: \{endpoint}",
                    e
            );
        }
    }

    private String buildGraphQLEndpoint(SchemaLocation schemaLocation) {
        final String clusterEndpoint = joinUrl(
                normalizeEndpoint(schemaLocation.getLocation()),
                normalizeContextPath(schemaLocation.getContextPath())
        );
        return joinUrl(clusterEndpoint, normalizePath(schemaLocation.getPathApi()));
    }

    private void copyHeaders(HttpHeaders headers) {
        List<String> headersToCopy = Context.get(PropertyNames.HEADERS_TO_COPY);
        headersToCopy.forEach(headerName -> {
            Object headerValue = Context.get(headerName);
            if (headerValue != null) {
                headers.add(headerName, headerValue.toString());
            }
        });
        Map<String, String> requestHeaders = Context.get(REQUEST_HEADERS);
        requestHeaders.forEach((key, value) -> headers.add(key, value));
    }
}
