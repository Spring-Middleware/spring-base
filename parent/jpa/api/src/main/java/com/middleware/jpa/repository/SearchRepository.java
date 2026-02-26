package com.middleware.jpa.repository;

import com.middleware.data.Pagination;
import com.middleware.jpa.search.Search;

import java.util.List;

public interface SearchRepository<T, S extends Search> {

    List<T> findBySearch(S search, Pagination pagination) throws Exception;

    Long countBySearch(S search) throws Exception;
}
