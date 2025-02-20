package com.core.jpa.repository;

import com.core.data.Pagination;
import com.core.jpa.buffer.QueryBuffer;
import com.core.jpa.buffer.QueryBufferParameters;
import com.core.jpa.order.OrderBy;
import com.core.jpa.pagination.PaginableResultDB;
import com.core.jpa.query.QueryFilter;
import com.core.jpa.query.QueryParameterizer;
import com.core.jpa.search.Search;
import com.core.jpa.types.OrderType;
import com.core.sort.SortedSearch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.log4j.Logger;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public class SearchRepositoryImpl<T, S extends Search> implements SearchRepository<T, S> {

    @PersistenceContext
    private EntityManager entityManager;

    private Logger logger = Logger.getLogger(SearchRepositoryImpl.class);

    @Override
    public Long countBySearch(S search) throws Exception {

        QueryFilter<T, S> queryFilter = new QueryFilter<>(search);
        if (!queryFilter.hasFilters()) {
            Query query = createQueryForSearch(search, null, queryFilter, Boolean.TRUE);
            return (Long) query.getSingleResult();
        } else {
            List<T> results = findBySearch(search, null);
            return (long) results.size();
        }
    }

    @Override
    public List<T> findBySearch(S search, Pagination pagination) throws Exception {

        QueryFilter queryFilter = new QueryFilter(search);
        Query query = createQueryForSearch(search, pagination, queryFilter, Boolean.FALSE);
        List<T> results = query.getResultList();
        queryFilter.applyFilters(results);
        if (pagination != null && queryFilter.hasFilters()) {
            PaginableResultDB paginableResultDB = new PaginableResultDB(results);
            results = paginableResultDB.paginate(pagination);
        }
        return results;
    }

    protected Query createQueryForSearch(S search, Pagination pagination, QueryFilter queryFilter, boolean isCount)
            throws Exception {

        SortedSearch sortedSearch = null;
        if (search instanceof SortedSearch) {
            sortedSearch = (SortedSearch) search;
        }
        QueryBufferParameters queryBufferParameters = new QueryBufferParameters(search,
                this.getEntityClass(),
                new OrderBy(Optional.ofNullable(sortedSearch).map(s -> s.getSortCriteria())
                        .map(c -> new ArrayList(c.getProperties()))
                        .orElse(null), Optional.ofNullable(sortedSearch).map(s -> s.getSortCriteria())
                        .map(c -> OrderType.valueOf(c.getDirection().name())).orElse(null)),
                isCount);

        QueryBuffer queryBuffer = new QueryBuffer(queryBufferParameters);
        logger.debug("Query: " + queryBuffer.toString());
        Query query = entityManager.createQuery(queryBuffer.toString());
        QueryParameterizer queryParameterizer = new QueryParameterizer();
        query = queryParameterizer.parameterizeQuery(query, search);
        Query pagedQuery = setPagination(query, pagination, queryFilter.hasFilters());
        Optional.ofNullable(search.lockModeType()).ifPresent(lockModeType -> pagedQuery.setLockMode(lockModeType));
        return pagedQuery;
    }

    private Query setPagination(Query query, Pagination pagination, boolean hasFilters) {

        if (pagination != null && !hasFilters) {
            if (pagination.getPageSize() >= 1 && pagination.getPageNumber() >= 0) {
                query.setFirstResult((pagination.getPageNumber()) * pagination.getPageSize());
                query.setMaxResults(pagination.getPageSize());
            }
        }
        return query;
    }

    private Class<S> getEntityClass() {

        return (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

}
