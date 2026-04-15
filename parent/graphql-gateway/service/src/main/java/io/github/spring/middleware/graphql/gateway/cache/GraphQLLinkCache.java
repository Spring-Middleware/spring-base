package io.github.spring.middleware.graphql.gateway.cache;

import java.util.concurrent.ConcurrentHashMap;

public class GraphQLLinkCache {

    private ConcurrentHashMap<GraphQLLinkCacheKey, Object> cache = new ConcurrentHashMap<>();

    public Object get(GraphQLLinkCacheKey graphQLLinkCacheKey) {
        return cache.get(graphQLLinkCacheKey);
    }

    public void put(GraphQLLinkCacheKey graphQLLinkCacheKey, Object result) {
        cache.put(graphQLLinkCacheKey, result);
    }

}
