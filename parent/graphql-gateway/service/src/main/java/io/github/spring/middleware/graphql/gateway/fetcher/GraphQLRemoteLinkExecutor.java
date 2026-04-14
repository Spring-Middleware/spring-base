package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLLinkBatched;
import io.github.spring.middleware.graphql.gateway.client.RemoteGraphQLExecutionClient;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.mapErrors;
import static io.github.spring.middleware.graphql.gateway.util.GraphQLUtils.normalizeValue;

@Component
public class GraphQLRemoteLinkExecutor {

    private RemoteGraphQLExecutionClient remoteGraphQLExecutionClient;

    public GraphQLRemoteLinkExecutor(RemoteGraphQLExecutionClient remoteGraphQLExecutionClient) {
        this.remoteGraphQLExecutionClient = remoteGraphQLExecutionClient;
    }

    public Object executeLink(DataFetchingEnvironment environment, GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink, ExecutionInput executionInput, List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks) {
        Map<String, Object> response = remoteGraphQLExecutionClient.execute(resolvedLink.getTargetSchemaLocation(), executionInput);
        if (response == null || response.isEmpty()) {
            return null;
        }

        Map<?, ?> dataMap = response.get("data") instanceof Map<?, ?> m ? m : null;
        Object fieldData = dataMap != null ? dataMap.get(resolvedLink.getFieldLinkDefinition().getQuery()) : null;

        Object normalizedData = normalizeValue(fieldData, environment, graphQLResolvedLinks);

        return DataFetcherResult.newResult()
                .data(normalizedData)
                .errors(mapErrors(response.get("errors"), environment))
                .build();
    }


    public CompletableFuture<Map<ItemKey, Object>> executeLink(ExecutionInput executionInput, GraphQLLinkBatched batchedLink) {
        DataFetcherResult<Object> dataFetcherResult = (DataFetcherResult) executeLink(batchedLink.getDataFetchingEnvironment(), batchedLink.getResolvedLink(), executionInput, List.of());

        Object data = dataFetcherResult.getData();

        if (!(data instanceof List<?> items)) {
            throw new IllegalStateException(
                    "Expected batch result list for link %s but got: %s"
                            .formatted(batchedLink.getResolvedLink(), data.getClass().getName())
            );
        }

        Map<ItemKey, Object> result = new LinkedHashMap<>();
        items.forEach(item -> {
            ItemKey itemKey = buildItemKey(batchedLink.getResolvedLink(), item);
            if (result.put(itemKey, item) != null) {
                throw new IllegalStateException(
                        "Duplicate batch key returned from remote execution: %s".formatted(itemKey)
                );
            }
        });
        return CompletableFuture.completedFuture(result);
    }

    private ItemKey buildItemKey(GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink, Object item) {
        Map<String, Object> args = new LinkedHashMap<>();
        GraphQLFieldLinkDefinition fieldLinkDefinition = resolvedLink.getFieldLinkDefinition();
        Optional.ofNullable(fieldLinkDefinition.getTargetTypeName())
                .ifPresent(targetTypeName -> {
                    if (!(item instanceof Map<?, ?> map)) {
                        throw new IllegalStateException(
                                "Expected Map for batch item but got: %s".formatted(item)
                        );
                    }
                    fieldLinkDefinition.getArgumentLinkDefinitions().stream()
                            .filter(GraphQLArgumentLinkDefinition::isBatched)
                            .sorted(Comparator.comparing(GraphQLArgumentLinkDefinition::getArgumentName))
                            .forEach(argDef -> {
                                String matchName = argDef.getTargetFieldName() != null ? argDef.getTargetFieldName() : argDef.getArgumentName();
                                if (!map.containsKey(matchName)) {
                                    throw new IllegalStateException(
                                            "Missing argument value for batch key: %s".formatted(argDef.getArgumentName())
                                    );
                                } else {
                                    Object value = map.get(matchName);
                                    if (value == null) {
                                        throw new IllegalStateException(
                                                "Null value for batch key argument: %s".formatted(argDef.getArgumentName())
                                        );
                                    } else {
                                        args.put(argDef.getArgumentName(), value);
                                    }
                                }
                            });
                });

        return new ItemKey(fieldLinkDefinition.getQuery(), args);
    }


    public record ItemKey(String query, Map<String, Object> args) {
    }

}
