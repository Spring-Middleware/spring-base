package com.middleware.mongo.repository;

import com.middleware.data.Pagination;
import com.middleware.mongo.search.GeoSearch;
import com.middleware.mongo.search.MongoSearch;
import org.springframework.data.geo.GeoResults;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MongoSearchReactiveRepository<T, S extends MongoSearch> {

    Flux<T> findBySearch(Mono<S> search, Mono<Pagination> pagination);

    Mono<Long> countBySearch(Mono<S> mongoSearch);

    Flux<T> aggregateBySearch(Mono<S> search, Mono<Pagination> pagination);

    Mono<Long> countByAggregateSearch(Mono<S> mongoSearch);

    Flux<GeoResults<T>> findByGeoSearch(Mono<S> mongoSearch, Mono<Pagination> pagination, Mono<GeoSearch> geoSearch);

}
