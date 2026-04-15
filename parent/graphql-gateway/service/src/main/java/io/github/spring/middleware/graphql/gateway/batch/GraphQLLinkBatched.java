package io.github.spring.middleware.graphql.gateway.batch;

import graphql.ExecutionInput;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class GraphQLLinkBatched {

    private ExecutionInput.Builder executionInputBuilder;
    private GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink;
    private Map<String, List<Object>> argumentValuesMap = new LinkedHashMap<>();
    private DataFetchingEnvironment dataFetchingEnvironment;
    private Map<String, Object> variablesNonBatching = new LinkedHashMap<>();
    private List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks = new ArrayList<>();
    private final Map<GraphQLLinkTypesMap.BatchKey, CompletableFuture<Object>> pending = new LinkedHashMap<>();
    private final AtomicBoolean executed = new AtomicBoolean(false);

    public GraphQLLinkBatched(ExecutionInput.Builder executionInputBuilder, GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink, DataFetchingEnvironment dataFetchingEnvironment) {
        this.executionInputBuilder = executionInputBuilder;
        this.resolvedLink = resolvedLink;
        this.dataFetchingEnvironment = dataFetchingEnvironment;
    }

    public void addArgumentValue(String argumentName, Object value) {
        argumentValuesMap
                .computeIfAbsent(argumentName, k -> new ArrayList<>())
                .add(value);
    }

    public List<Object> getArgumentValues(String argumentName) {
        return argumentValuesMap.getOrDefault(argumentName, List.of());
    }

    public Map<String, List<Object>> getArgumentValuesMap() {
        return argumentValuesMap;
    }

    public CompletableFuture<Object> register(GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink, Map<String, Object> variables, List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks) {

        this.graphQLResolvedLinks.addAll(graphQLResolvedLinks);
        GraphQLLinkTypesMap.BatchKey key = resolvedLink.getBatchKey(variables);
        return pending.computeIfAbsent(key, k -> {
            // solo cuando es nueva key acumulas
            variables.forEach((arg, val) -> {
                if (resolvedLink.isBatcheArgument(arg)) {
                    List<Object> values = argumentValuesMap.computeIfAbsent(arg, x -> new ArrayList<>());

                    if (val instanceof Collection<?> collection) {
                        values.addAll(collection);
                    } else {
                        values.add(val);
                    }
                } else {
                    // si no es batch, lo guardo aparte para agregarlo a la query al momento de ejecutar el batch
                    variablesNonBatching.putIfAbsent(arg, val);
                }
            });
            return new CompletableFuture<>();
        });
    }

    public boolean markExecutedIfNeeded() {
        return executed.compareAndSet(false, true);
    }

    public boolean isExecuted() {
        return executed.get();
    }

    public ExecutionInput.Builder getExecutionInputBuilder() {
        return executionInputBuilder;
    }

    public Map<GraphQLLinkTypesMap.BatchKey, CompletableFuture<Object>> getPending() {
        return pending;
    }

    public void completePending(GraphQLLinkTypesMap.BatchKey batchKey, Object value) {
        CompletableFuture<Object> future = pending.get(batchKey);
        if (future != null) {
            future.complete(value);
        }
    }

    public void completePendingExceptionally(Throwable ex) {
        pending.values().forEach(future -> future.completeExceptionally(ex));
    }


    public DataFetchingEnvironment getDataFetchingEnvironment() {
        return dataFetchingEnvironment;
    }

    public GraphQLLinkTypesMap.GraphQLResolvedLink getResolvedLink() {
        return resolvedLink;
    }

    public List<GraphQLLinkTypesMap.BatchKey> getBatchKeys() {
        return new ArrayList<>(pending.keySet());
    }

    public Map<String, Object> getVariablesNonBatching() {
        return variablesNonBatching;
    }

    public List<GraphQLLinkTypesMap.GraphQLResolvedLink> getGraphQLResolvedLinks() {
        return graphQLResolvedLinks;
    }
}
