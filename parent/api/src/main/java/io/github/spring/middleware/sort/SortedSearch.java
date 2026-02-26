package io.github.spring.middleware.sort;

public interface SortedSearch {

    default SortCriteria getSortCriteria() {

        return null;
    }

}
