package io.github.spring.middleware.jpa.repository;

import io.github.spring.middleware.data.Pagination;
import io.github.spring.middleware.jpa.search.Search;

import java.util.List;

public interface SearchRepository<T, S extends Search> {

    List<T> findBySearch(S search, Pagination pagination) throws Exception;

    Long countBySearch(S search) throws Exception;
}
