package com.middleware.mongo.components;

import com.middleware.mongo.search.MongoSearch;
import com.middleware.sort.SortedSearch;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class SortBuilderComponent<S extends MongoSearch> {

    public SortOperation buildSortOperation(S mongoSearch) {

        SortOperation sortOperation = null;
        if (mongoSearch instanceof SortedSearch) {
            SortedSearch mongoSortedSearch = (SortedSearch) mongoSearch;
            sortOperation = Optional.ofNullable(mongoSortedSearch.getSortCriteria()).map(sortCriteria -> {
                String[] properties = Optional.ofNullable(sortCriteria.getProperties())
                        .map(p -> p.toArray(new String[sortCriteria.getProperties().size()])).orElse(null);
                return Optional.ofNullable(properties).map(prop -> {
                    return Aggregation.sort(Sort.by(Optional.ofNullable(sortCriteria.getDirection()).orElse(
                            Sort.Direction.ASC), prop));
                }).orElse(null);
            }).orElse(null);
        }
        return sortOperation;
    }

    public Mono<SortOperation> buildSortOperationAsync(Mono<S> mongoSearch) {

        return mongoSearch.flatMap(search -> Mono.justOrEmpty(buildSortOperation(search)));
    }

    public Sort buildSort(S mongoSearch) {

        Sort sort = null;
        if (mongoSearch instanceof SortedSearch) {
            SortedSearch mongoSortedSearch = (SortedSearch) mongoSearch;
            sort = Optional.ofNullable(mongoSortedSearch.getSortCriteria()).map(sortCriteria -> {
                String[] properties = sortCriteria.getProperties()
                        .toArray(new String[sortCriteria.getProperties().size()]);
                return Sort.by(sortCriteria.getDirection(), properties);
            }).orElse(null);
        }
        return sort;
    }

    public Mono<Sort> buildSortAsync(Mono<S> mongoSearch) {

        return mongoSearch.flatMap(search -> Mono.justOrEmpty(buildSort(search)));
    }

}
