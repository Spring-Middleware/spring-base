package io.github.spring.middleware.graphql.gateway.batch;

import graphql.ExecutionInput;
import io.github.spring.middleware.graphql.gateway.fetcher.GraphQLRemoteLinkExecutor;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphQLBatchExecutor {

    private Logger logger = LoggerFactory.getLogger(GraphQLBatchExecutor.class);

    private final GraphQLRemoteLinkExecutor remoteLinkExecutor;

    public GraphQLBatchExecutor(GraphQLRemoteLinkExecutor remoteLinkExecutor) {
        this.remoteLinkExecutor = remoteLinkExecutor;
    }

    public CompletableFuture<Void> executeBatch(GraphQLLinkBatched batchedLink) {

        if (!batchedLink.hasPending()) {
            return CompletableFuture.completedFuture(null);
        }

        ExecutionInput executionInput = batchedLink.getExecutionInputBuilder()
                .variables(buildAggregatedVariables(batchedLink))
                .build();

        return remoteLinkExecutor.executeLink(executionInput, batchedLink)
                .handle((result, ex) -> {

                    if (ex != null) {
                        logger.error("Error executing batch for link: {}", batchedLink.getResolvedLink(), ex);
                        batchedLink.completePendingExceptionally(ex);
                        return null;
                    }

                    Map<GraphQLRemoteLinkExecutor.ItemKey, Integer> remoteOrder = new HashMap<>();

                    int index = 0;
                    for (GraphQLRemoteLinkExecutor.ItemKey resultKey : result.keySet()) {
                        remoteOrder.put(resultKey, index++);
                    }

                    for (GraphQLLinkTypesMap.BatchKey batchKey : batchedLink.getBatchKeys()) {
                        List<GraphQLRemoteLinkExecutor.ItemKey> itemKeys =
                                batchKey.splitIntoItemKeys(batchedLink.getResolvedLink());

                        List<GraphQLRemoteLinkExecutor.ItemKey> orderedItemKeys = itemKeys.stream()
                                .sorted(Comparator.comparingInt(key ->
                                        remoteOrder.getOrDefault(key, Integer.MAX_VALUE)
                                ))
                                .toList();

                        List<Object> items = orderedItemKeys.stream()
                                .map(itemKey -> {
                                    Object value = result.get(itemKey);
                                    if (value == null) {
                                        logger.warn(
                                                "Batch result missing item key: {} for batch key: {} in link: {}",
                                                itemKey, batchKey, batchedLink.getResolvedLink()
                                        );
                                    }
                                    return value;
                                })
                                .filter(Objects::nonNull)
                                .toList();

                        batchedLink.completePending(batchKey, items);
                    }
                    return null;
                });
    }


    private Map<String, Object> buildAggregatedVariables(GraphQLLinkBatched batchedLink) {
        Map<String, Object> aggregatedVariables = new LinkedHashMap<>();
        batchedLink.getArgumentValuesMap().forEach((argName, values) -> {
            // aquí puedes decidir cómo agregar los valores, por ejemplo, como una lista
            aggregatedVariables.put(argName, values);
        });
        batchedLink.getVariablesNonBatching().forEach(aggregatedVariables::put);
        return aggregatedVariables;
    }

}
