package io.github.spring.middleware.mongo.search;

import io.github.spring.middleware.sort.SortCriteria;
import io.github.spring.middleware.sort.SortedSearch;
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

