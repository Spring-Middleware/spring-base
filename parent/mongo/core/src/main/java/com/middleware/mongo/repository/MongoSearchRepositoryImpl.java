package com.middleware.mongo.repository;

import com.middleware.data.Pagination;
import com.middleware.mongo.components.CriteriaBuilderComponent;
import com.middleware.mongo.components.QueryCreatorComponent;
import com.middleware.mongo.components.SortBuilderComponent;
import com.middleware.mongo.data.TotalCount;
import com.middleware.mongo.exception.MongoSearchException;
import com.middleware.mongo.search.Coordinates;
import com.middleware.mongo.search.GeoSearch;
import com.middleware.mongo.search.MongoSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

@Slf4j
@NoRepositoryBean
public class MongoSearchRepositoryImpl<T, S extends MongoSearch> implements MongoSearchRepository<T, S> {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private QueryCreatorComponent queryCreator;
    @Autowired
    private CriteriaBuilderComponent criteriaBuilder;
    @Autowired
    private SortBuilderComponent sortBuilder;
    @Autowired
    private PipeLineBuilderComponent pipeLineBuilder;

    public List<T> findBySearch(S mongoSearch, Pagination pagination) {

        try {
            Class<T> entityClazz = (Class) getEntityClass();
            Query query = queryCreator.createQuery(mongoSearch);
            Criteria criteria = criteriaBuilder.buildCriteria(mongoSearch, new Criteria(), "");
            if (log.isDebugEnabled()) {
                log.debug(Query.query(criteria).toString());
            }
            Optional.ofNullable(criteria).ifPresent(cr -> query.addCriteria(cr));
            Sort sort = sortBuilder.buildSort(mongoSearch);
            Optional.ofNullable(sort).ifPresent(s -> query.with(s));
            if (pagination != null) {
                query.with(PageRequest.of(pagination.getPageNumber(), pagination.getPageSize()));
            }
            return mongoTemplate.find(query, entityClazz, StringUtils.uncapitalize(entityClazz.getSimpleName()));
        } catch (
                Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public List<T> aggregateBySearch(S mongoSearch, Pagination pagination) {

        try {
            List<AggregationOperation> aggregationOperations = pipeLineBuilder
                    .buildPipeline(mongoSearch, pagination, false);
            Class<T> entityClazz = (Class) getEntityClass();
            return mongoTemplate.aggregate(Aggregation.newAggregation(aggregationOperations),
                    StringUtils.uncapitalize(entityClazz.getSimpleName()), entityClazz).getMappedResults();
        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public long countByAggregateSearch(S mongoSearch) {

        try {
            List<AggregationOperation> aggregationOperations = pipeLineBuilder.buildPipeline(mongoSearch, null, true);
            Class<T> entityClazz = (Class) getEntityClass();
            TotalCount totalCount = mongoTemplate.aggregate(Aggregation.newAggregation(aggregationOperations),
                    StringUtils.uncapitalize(entityClazz.getSimpleName()), TotalCount.class).getMappedResults().stream()
                    .findFirst().orElseGet(TotalCount::new);
            return totalCount.getTotal();

        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public long countBySearch(S mongoSearch) {

        try {
            Class<T> entityClazz = (Class) getEntityClass();
            Query query = queryCreator.createQuery(mongoSearch);
            Criteria criteria = criteriaBuilder.buildCriteria(mongoSearch, new Criteria(), "");
            Optional.ofNullable(criteria).ifPresent(cr -> query.addCriteria(cr));
            return mongoTemplate.count(query, entityClazz, StringUtils.uncapitalize(entityClazz.getSimpleName()));
        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public GeoResults<T> findByGeoSearch(S mongoSearch, Pagination pagination, GeoSearch geoSearch) {

        try {
            Class<T> entityClazz = (Class) getEntityClass();
            Query query = queryCreator.createQuery(mongoSearch);
            Criteria criteria = criteriaBuilder.buildCriteria(mongoSearch, new Criteria(), "");
            Optional.ofNullable(criteria).ifPresent(cr -> query.addCriteria(cr));
            Coordinates coordinates = geoSearch.getCoordinates();
            NearQuery nearQuery = NearQuery.near(coordinates.getLatitude(), coordinates.getLongitude(),
                    Metrics.KILOMETERS);
            nearQuery.query(query);
            Optional.ofNullable(geoSearch.getMinDistance()).ifPresent(d -> nearQuery.minDistance(d));
            Optional.ofNullable(geoSearch.getMaxDistance()).ifPresent(d -> nearQuery.maxDistance(d));
            nearQuery.spherical(true);
            if (pagination != null) {
                nearQuery.with(PageRequest.of(pagination.getPageNumber(), pagination.getPageSize()));
            }
            return mongoTemplate
                    .geoNear(nearQuery, entityClazz, StringUtils.uncapitalize(entityClazz.getSimpleName()));

        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    private Class<T> getEntityClass() {

        return (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

}
