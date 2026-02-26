package com.middleware.mongo.repository;

import com.middleware.data.Pagination;
import com.middleware.mongo.search.GeoSearch;
import com.middleware.mongo.search.MongoSearch;
import org.springframework.data.geo.GeoResults;

import java.util.List;

public interface MongoSearchRepository<T, S extends MongoSearch> {

    List<T> findBySearch(S search, Pagination pagination);

    long countBySearch(S mongoSearch);

    List<T> aggregateBySearch(S search, Pagination pagination);

    long countByAggregateSearch(S mongoSearch);

    GeoResults<T> findByGeoSearch(S mongoSearch, Pagination pagination, GeoSearch geoSearch);



}
