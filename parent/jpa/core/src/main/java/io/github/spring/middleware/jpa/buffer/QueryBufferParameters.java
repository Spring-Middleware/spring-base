package io.github.spring.middleware.jpa.buffer;

import io.github.spring.middleware.jpa.order.OrderBy;
import io.github.spring.middleware.jpa.search.Search;

public class QueryBufferParameters<T, S extends Search> {

    private final S search;
    private final Class<T> entityClazz;
    private final OrderBy orderBy;
    private final boolean isCount;

    public QueryBufferParameters(Class<T> entityClazz, OrderBy orderBy) {

        this(null, entityClazz, orderBy, false);
    }

    public QueryBufferParameters(S search, Class<T> entityClazz, OrderBy orderBy, boolean isCount) {

        this.search = search;
        this.entityClazz = entityClazz;
        this.orderBy = orderBy;
        this.isCount = isCount;
    }

    public S getSearch() {

        return search;
    }

    public Class<T> getEntityClazz() {

        return entityClazz;
    }

    public OrderBy getOrderBy() {

        return orderBy;
    }

    public boolean isCount() {

        return isCount;
    }
}
