package com.middleware.sort;

public interface SortedSearch {

    default SortCriteria getSortCriteria() {

        return null;
    }

}
