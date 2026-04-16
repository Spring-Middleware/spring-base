package io.github.spring.middleware.graphql.gateway.batch;

import graphql.ExecutionInput;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphQLLinkResolvedBatchedRegistry {

    private Map<GraphQLLinkTypesMap.GraphQLResolvedLink, GraphQLLinkBatched> resolvedBatchedLinkMap = new LinkedHashMap<>();

    public GraphQLLinkBatched getOrCreate(GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink, ExecutionInput.Builder executionInputBuilder, DataFetchingEnvironment dataFetchingEnvironment) {
        return resolvedBatchedLinkMap.computeIfAbsent(resolvedLink, key -> new GraphQLLinkBatched(executionInputBuilder, resolvedLink, dataFetchingEnvironment));
    }

    public Map<GraphQLLinkTypesMap.GraphQLResolvedLink, GraphQLLinkBatched> getResolvedBatchedLinkMap() {
        return resolvedBatchedLinkMap;
    }

    public List<GraphQLLinkBatched> getAllBatchedLinks() {
        return List.copyOf(resolvedBatchedLinkMap.values());
    }

    public List<GraphQLLinkBatched> getPendingBatches() {
       return resolvedBatchedLinkMap.values().stream().filter(GraphQLLinkBatched::hasPending).toList();
    }

}
