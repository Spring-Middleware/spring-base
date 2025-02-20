package com.core.jpa.repository;

import com.core.data.Pagination;
import com.core.jpa.search.Search;

import java.util.List;

public interface SearchRepository<T, S extends Search> {

    List<T> findBySearch(S search, Pagination pagination) throws Exception;

    Long countBySearch(S search) throws Exception;
}
