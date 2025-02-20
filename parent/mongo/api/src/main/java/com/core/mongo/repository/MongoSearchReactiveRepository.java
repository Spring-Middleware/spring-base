package com.core.mongo.repository;

import com.core.data.Pagination;
import com.core.mongo.search.GeoSearch;
import com.core.mongo.search.MongoSearch;
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
