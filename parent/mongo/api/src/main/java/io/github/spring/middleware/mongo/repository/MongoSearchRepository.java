package io.github.spring.middleware.mongo.repository;

import io.github.spring.middleware.data.Pagination;
import io.github.spring.middleware.mongo.search.GeoSearch;
import io.github.spring.middleware.mongo.search.MongoSearch;
import org.springframework.data.geo.GeoResults;

import java.util.List;

public interface MongoSearchRepository<T, S extends MongoSearch> {

    List<T> findBySearch(S search, Pagination pagination);

    long countBySearch(S mongoSearch);

    List<T> aggregateBySearch(S search, Pagination pagination);

    long countByAggregateSearch(S mongoSearch);

    GeoResults<T> findByGeoSearch(S mongoSearch, Pagination pagination, GeoSearch geoSearch);



}
