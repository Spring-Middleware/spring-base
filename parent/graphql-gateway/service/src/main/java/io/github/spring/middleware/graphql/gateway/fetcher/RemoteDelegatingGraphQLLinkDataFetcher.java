package io.github.spring.middleware.graphql.gateway.fetcher;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLLinkBatched;
import io.github.spring.middleware.graphql.gateway.batch.GraphQLLinkResolvedBatchedRegistry;
import io.github.spring.middleware.graphql.gateway.cache.GraphQLCachingToggle;
import io.github.spring.middleware.graphql.gateway.cache.GraphQLLinkCache;
import io.github.spring.middleware.graphql.gateway.cache.GraphQLLinkCacheKey;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;
import io.github.spring.middleware.graphql.gateway.fetcher.builder.QueryLinkBuilder;
import io.github.spring.middleware.graphql.gateway.loader.GraphQLLinkTypesMap;
import io.github.spring.middleware.graphql.gateway.runtime.GraphQLBatchingToggle;
import io.github.spring.middleware.graphql.metadata.GraphQLArgumentLinkDefinition;
import io.github.spring.middleware.graphql.metadata.GraphQLFieldLinkDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.spring.middleware.graphql.gateway.util.GraphQLSourceFieldExtractor.extractFieldValue;

public class RemoteDelegatingGraphQLLinkDataFetcher implements DataFetcher<Object> {

    private GraphQLLinkTypesMap graphQLLinkTypesMap;
    private GraphQLRemoteLinkExecutor graphQLRemoteLinkExecutor;
    private GraphQLBatchingToggle batchingToggle;
    private GraphQLCachingToggle cachingToggle;

    public RemoteDelegatingGraphQLLinkDataFetcher(GraphQLLinkTypesMap graphQLLinkTypesMap, GraphQLRemoteLinkExecutor graphQLRemoteLinkExecutor, GraphQLBatchingToggle batchingToggle, GraphQLCachingToggle cachingToggle) {
        this.graphQLLinkTypesMap = graphQLLinkTypesMap;
        this.graphQLRemoteLinkExecutor = graphQLRemoteLinkExecutor;
        this.batchingToggle = batchingToggle;
        this.cachingToggle = cachingToggle;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        String typeName = environment.getExecutionStepInfo().getObjectType().getName();
        String fieldName = environment.getFieldDefinition().getName();

        GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink = graphQLLinkTypesMap.findGraphQLResolvedLink(typeName, fieldName);
        if (resolvedLink == null) {
            throw new IllegalStateException(
                    STR."No resolved link found for type: \{typeName}, field: \{fieldName}"
            );
        }


        Object source = environment.getSource();
        Object extractedValue = extractFieldValue(source, resolvedLink.getFieldLinkDefinition().getFieldName());

        Map<String, Object> variables = new HashMap<>();

        List<GraphQLArgumentLinkDefinition> args =
                resolvedLink.getFieldLinkDefinition().getArgumentLinkDefinitions();

        if (args.size() == 1 && !(extractedValue instanceof Map)) {
            // caso simple (ids, etc.)
            if (extractedValue != null) {
                variables.put(args.get(0).getArgumentName(), extractedValue);
            }
        } else {
            // caso múltiple (Map)
            if (!(extractedValue instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        STR."Expected Map for multiple arguments but got: \{extractedValue}"
                );
            }

            for (GraphQLArgumentLinkDefinition argDef : args) {
                String argName = argDef.getArgumentName();
                Object value = map.get(argName);
                if (value == null) {
                    throw new IllegalStateException(
                            STR."Missing argument value for: \{argName}"
                    );
                }
                if (value != null) {
                    variables.put(argName, value);
                }
            }
        }

        List<GraphQLLinkTypesMap.GraphQLResolvedLink> graphQLResolvedLinks = graphQLLinkTypesMap.findGraphQLResolvedLinksForSchemaAndTypeName(resolvedLink.getTargetSchemaLocation(), getReturnedTypeName(environment));
        List<GraphQLLinkTypesMap.GraphQLResolvedLink> normalizedLinks = graphQLResolvedLinks.stream()
                .filter(link -> link.getOriginOperationReturnType() != null)
                .toList();

        Map<String, GraphQLLinkTypesMap.GraphQLResolvedLink> resolvedLinksByFieldName =
                normalizedLinks.stream()
                        .collect(Collectors.toMap(
                                GraphQLLinkTypesMap.GraphQLResolvedLink::getFieldName,
                                Function.identity(),
                                (left, right) -> left
                        ));

        QueryLinkBuilder queryLinkBuilder = new QueryLinkBuilder();
        queryLinkBuilder.appendGraphQLQuery(environment, resolvedLink, variables, resolvedLinksByFieldName);
        String query = queryLinkBuilder.build();

        ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(resolvedLink.getFieldLinkDefinition().getQuery())
                .graphQLContext(builder -> {
                    GraphQLContext originalContext = environment.getGraphQlContext();
                    builder.of(originalContext);
                });

        if (resolvedLink.isBatched() && batchingToggle.isEnabled()) {
            GraphQLLinkResolvedBatchedRegistry batchedRegistry = getOrCreateBatchedRegistry(environment, resolvedLink);
            GraphQLLinkBatched graphQLLinkBatched = batchedRegistry.getOrCreate(resolvedLink, executionInputBuilder, environment);
            if (hasExecutableBatchArguments(resolvedLink.getFieldLinkDefinition(), variables)) {
                return graphQLLinkBatched.register(resolvedLink, variables, normalizedLinks);
            } else {
                return resolvedLink.getFieldLinkDefinition().isCollection() ? List.of() : null;
            }
        } else {
            ExecutionInput executionInput = executionInputBuilder.variables(variables).build();
            GraphQLLinkCache graphQLLinkCache = getOrCreateCache(environment);
            if (cachingToggle.isEnabled() && !variables.isEmpty()) {
                GraphQLLinkCacheKey cacheKey = new GraphQLLinkCacheKey(resolvedLink.getTargetSchemaLocation().getNamespace(), query, variables);
                Object cachedResult = graphQLLinkCache.get(cacheKey);
                if (cachedResult != null) {
                    return cachedResult;
                }
                // Para enlaces no batched, ejecutamos inmediatamente
                Object result = graphQLRemoteLinkExecutor.executeLink(environment, resolvedLink, executionInput, normalizedLinks);
                graphQLLinkCache.put(cacheKey, result);
                return result;
            } else if (!variables.isEmpty()) {
                return graphQLRemoteLinkExecutor.executeLink(environment, resolvedLink, executionInput, normalizedLinks);
            } else {
                return null;
            }
        }
    }

    private boolean hasExecutableBatchArguments(GraphQLFieldLinkDefinition fieldLinkDefinition, Map<String, Object> variables) {
        return fieldLinkDefinition.getArgumentLinkDefinitions().stream()
                .filter(GraphQLArgumentLinkDefinition::isBatched)
                .map(argDef -> variables.get(argDef.getArgumentName()))
                .anyMatch(value -> {
                    if (value == null) {
                        return false;
                    }
                    if (value instanceof Collection<?> collection) {
                        return !collection.isEmpty();
                    }
                    return true;
                });
    }


    private GraphQLLinkResolvedBatchedRegistry getOrCreateBatchedRegistry(DataFetchingEnvironment environment, GraphQLLinkTypesMap.GraphQLResolvedLink resolvedLink) {
        GraphQLLinkResolvedBatchedRegistry graphQLLinkResolvedBatchedRegistry = environment.getGraphQlContext().computeIfAbsent("batchedRegistry", key -> new GraphQLLinkResolvedBatchedRegistry());
        return graphQLLinkResolvedBatchedRegistry;
    }

    private GraphQLLinkCache getOrCreateCache(DataFetchingEnvironment environment) {
        GraphQLLinkCache graphQLLinkCache = environment.getGraphQlContext().computeIfAbsent("linkCache", key -> new GraphQLLinkCache());
        return graphQLLinkCache;
    }

    private String getReturnedTypeName(DataFetchingEnvironment environment) {
        GraphQLType fieldType = environment.getFieldDefinition().getType();
        GraphQLType unwrappedType = GraphQLTypeUtil.unwrapAll(fieldType);

        if (!(unwrappedType instanceof GraphQLNamedType namedType)) {
            throw new GraphQLException(
                    GraphQLErrorCodes.REMOTE_EXECUTION_ERROR,
                    STR."Unsupported return type for remote execution: \{fieldType.getClass()}"
            );
        }
        String typeName = namedType.getName();
        return typeName;
    }

}
