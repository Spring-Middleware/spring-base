package com.commons.mongo.react.repository;

import com.middleware.data.Pagination;
import com.middleware.mongo.components.CriteriaBuilderComponent;
import com.middleware.mongo.components.QueryCreatorComponent;
import com.middleware.mongo.components.SortBuilderComponent;
import com.middleware.mongo.exception.MongoSearchException;
import com.middleware.mongo.repository.MongoSearchReactiveRepository;
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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

@Slf4j
@Service
@Component
public class MongoSearchReactiveRepositoryImpl<T, S extends MongoSearch> implements MongoSearchReactiveRepository<T, S> {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
    @Autowired
    private QueryCreatorComponent queryCreator;
    @Autowired
    private SortBuilderComponent sortBuilder;
    @Autowired
    private PipeLineReactiveBuilderComponent pipeLineBuilder;
    @Autowired
    private CriteriaBuilderComponent criteriaBuilder;

    public Flux<T> findBySearch(Mono<S> mongoSearch, Mono<Pagination> pagination) {

        try {
            Mono<Query> createQueryWithCriteria = createQueryWithCriteria(mongoSearch);
            Mono<Sort> sortMono = sortBuilder.buildSortAsync(mongoSearch);
            Mono<Query> addSortToQuery = createQueryWithCriteria.map(q -> {
                Sort sort = sortMono.block();
                if (sort != null) {
                    q.with(sort);
                }
                return q;
            });
            Mono<PageRequest> pageRequestMono = Optional.ofNullable(pagination)
                    .map(paginationMono -> paginationMono.flatMap(p -> {
                        PageRequest pageRequest = null;
                        if (p != null) {
                            pageRequest = PageRequest.of(p.getPageNumber(), p.getPageSize());
                        }
                        return Mono.justOrEmpty(pageRequest);
                    })).orElse(Mono.empty());

            Mono<Query> addPageRequestToSortedQuery = addSortToQuery.map(q -> {
                PageRequest pageRequest = pageRequestMono.block();
                if (pageRequest != null) {
                    q.with(pageRequest);
                }
                return q;
            });
            return mongoTemplate
                    .find(addPageRequestToSortedQuery.block(), getEntityClass(),
                            StringUtils.uncapitalize(getEntityClass().getSimpleName()));
        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    private Mono<Query> createQueryWithCriteria(Mono<S> mongoSearch) {

        Mono<Query> createQuery = queryCreator.createQueryAsync(mongoSearch);
        Mono<Criteria> criteriaMono = criteriaBuilder.buildCriteriaAsync(mongoSearch, new Criteria(), "");
        return createQuery.map(q -> {
            Criteria criteria = criteriaMono.block();
            if (log.isDebugEnabled()) {
                log.debug(Query.query(criteria).toString());
            }
            return q.addCriteria(criteria);
        });
    }

    public Mono<Long> countBySearch(Mono<S> mongoSearch) {

        try {
            Mono<Query> createQuery = queryCreator.createQueryAsync(mongoSearch);
            Mono<Criteria> criteriaMono = criteriaBuilder.buildCriteriaAsync(mongoSearch, new Criteria(), "");
            Mono<Query> addCriteriaToQuery = createQuery.map(q -> {
                Criteria criteria = criteriaMono.block();
                if (log.isDebugEnabled()) {
                    log.debug(Query.query(criteria).toString());
                }
                return q.addCriteria(criteria);
            });
            return mongoTemplate.count(addCriteriaToQuery.block(), getEntityClass(),
                    StringUtils.uncapitalize(getEntityClass().getSimpleName()));
        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public Flux<T> aggregateBySearch(Mono<S> mongoSearch, Mono<Pagination> pagination) {

        try {
            Flux<AggregationOperation> aggregationOperations = pipeLineBuilder
                    .buildPipeline(mongoSearch, pagination, false);
            Class<T> entityClazz = (Class) getEntityClass();
            return mongoTemplate.aggregate(Aggregation.newAggregation(aggregationOperations.collectList().block()),
                    StringUtils.uncapitalize(entityClazz.getSimpleName()), entityClazz);
        } catch (
                Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public Mono<Long> countByAggregateSearch(Mono<S> mongoSearch) {

        try {
            Flux<AggregationOperation> aggregationOperations = pipeLineBuilder.buildPipeline(mongoSearch, null, true);
            Class<T> entityClazz = (Class) getEntityClass();
            return mongoTemplate.aggregate(Aggregation.newAggregation(aggregationOperations.collectList().block()),
                    StringUtils.uncapitalize(entityClazz.getSimpleName()), entityClazz).count();
        } catch (
                Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    public Flux<GeoResults<T>> findByGeoSearch(Mono<S> mongoSearch, Mono<Pagination> pagination,
                                               Mono<GeoSearch> geoSearch) {

        try {
            Mono<Query> createQueryWithCriteria = createQueryWithCriteria(mongoSearch);
            Mono<NearQuery> nearQueryMono = geoSearch.map(geo -> {
                Coordinates coordinates = geo.getCoordinates();
                NearQuery nearQuery = NearQuery.near(coordinates.getLatitude(), coordinates.getLongitude(),
                        Metrics.KILOMETERS);
                nearQuery.query(createQueryWithCriteria.block());
                Optional.ofNullable(geo.getMinDistance()).ifPresent(d -> nearQuery.minDistance(d));
                Optional.ofNullable(geo.getMaxDistance()).ifPresent(d -> nearQuery.maxDistance(d));
                nearQuery.spherical(true);
                return nearQuery;
            });
            Mono<NearQuery> paginatedNearQuery = pagination.map(pag -> {
                return nearQueryMono.block().with(PageRequest.of(pag.getPageNumber(), pag.getPageSize()));
            });
            return (Flux) mongoTemplate
                    .geoNear(paginatedNearQuery.block(), getEntityClass(),
                            StringUtils.uncapitalize(getEntityClass().getSimpleName()));

        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

    private Class<T> getEntityClass() {

        return (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
