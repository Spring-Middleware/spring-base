package io.github.spring.middleware.commons.mongo.react.repository;

import io.github.spring.middleware.data.Pagination;
import io.github.spring.middleware.mongo.components.AddFieldsBuilderComponent;
import io.github.spring.middleware.mongo.components.CriteriaBuilderComponent;
import io.github.spring.middleware.mongo.components.SortBuilderComponent;
import io.github.spring.middleware.mongo.exception.MongoSearchException;
import io.github.spring.middleware.mongo.search.MongoSearch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PipeLineReactiveBuilderComponent<S extends MongoSearch> {

    @Autowired
    private SortBuilderComponent sortBuilder;
    @Autowired
    private AddFieldsBuilderComponent addFieldsBuilder;
    @Autowired
    private CriteriaBuilderComponent criteriaBuilder;

    public Flux<AggregationOperation> buildPipeline(Mono<S> mongoSearch, Mono<Pagination> pagination, boolean count) {

        try {
            Mono<Criteria> criteriaMono = criteriaBuilder.buildCriteriaAsync(mongoSearch, new Criteria(), "");
            Mono<MatchOperation> matchOperationMono = criteriaMono.map(c -> {
                if (log.isDebugEnabled()) {
                    log.debug(Query.query(c).toString());
                }
                return Aggregation.match(c);
            });
            Mono<AddFieldsOperation> addFieldsOperationMono = addFieldsBuilder.buildAddFieldsAsync(mongoSearch)
                    .doOnNext(operation -> {
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    ((AddFieldsOperation) operation).toDocument(Aggregation.DEFAULT_CONTEXT).toJson());
                        }
                    });
            Mono<SortOperation> sortOperationMono = sortBuilder.buildSortOperationAsync(mongoSearch);
            Mono<SkipOperation> skipOperationMono = pagination
                    .map(p -> Aggregation.skip((long) p.getPageNumber() * p.getPageSize()));
            Mono<LimitOperation> limitOperationMono = pagination.map(p -> Aggregation.limit(p.getPageSize()));
            Mono<CountOperation> countOperationMono = mongoSearch.filter(s -> count)
                    .map(s -> Aggregation.count().as("total"));
            return Flux.merge(addFieldsOperationMono, matchOperationMono, sortOperationMono, skipOperationMono,
                    limitOperationMono, countOperationMono);
        } catch (
                Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

}
