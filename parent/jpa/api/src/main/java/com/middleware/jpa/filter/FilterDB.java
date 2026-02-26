package com.middleware.jpa.filter;

import com.middleware.jpa.annotations.SearchFilter;

import java.util.Collection;

public interface FilterDB<T, P> {

    void prepare(Collection<T> entites, P p);

    boolean apply(SearchFilter searchFilter, T t, P p);
}
