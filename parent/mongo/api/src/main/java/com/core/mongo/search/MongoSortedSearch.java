package com.core.mongo.search;

import com.core.sort.SortCriteria;
import com.core.sort.SortedSearch;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class MongoSortedSearch implements MongoSearch, SortedSearch {

    private SortCriteria sortCriteria;

    public MongoSortedSearch(SortCriteria sortCriteria) {

        this.sortCriteria = sortCriteria;
    }

}

