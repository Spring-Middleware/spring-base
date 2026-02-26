package io.github.spring.middleware.jpa.filter;

import io.github.spring.middleware.jpa.annotations.SearchFilter;

import java.util.Collection;

public interface FilterDB<T, P> {

    void prepare(Collection<T> entites, P p);

    boolean apply(SearchFilter searchFilter, T t, P p);
}
