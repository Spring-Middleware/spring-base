package io.github.spring.middleware.mongo.repository;

import io.github.spring.middleware.data.Pagination;
import io.github.spring.middleware.mongo.components.AddFieldsBuilderComponent;
import io.github.spring.middleware.mongo.components.CriteriaBuilderComponent;
import io.github.spring.middleware.mongo.components.MaxSizeBuilderComponent;
import io.github.spring.middleware.mongo.components.SortBuilderComponent;
import io.github.spring.middleware.mongo.exception.MongoSearchException;
import io.github.spring.middleware.mongo.search.MongoSearch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PipeLineBuilderComponent<S extends MongoSearch> {

    @Autowired
    private CriteriaBuilderComponent criteriaBuilder;
    @Autowired
    private AddFieldsBuilderComponent addFieldsBuilder;
    @Autowired
    private SortBuilderComponent sortBuilder;
    @Autowired
    private MaxSizeBuilderComponent maxSizeBuilder;

    public List<AggregationOperation> buildPipeline(S mongoSearch, Pagination pagination, boolean count) {

        try {
            List<AggregationOperation> aggregationOperations = new ArrayList<>();
            Criteria searchCriteria = criteriaBuilder.buildCriteria(mongoSearch, new Criteria(), "");
            if (log.isDebugEnabled()) {
                log.debug(Query.query(searchCriteria).toString());
            }
            //AddFields
            AddFieldsOperation addFieldsOperation = addFieldsBuilder.buildAddFields(mongoSearch);
            Optional.ofNullable(addFieldsOperation).ifPresent(operation -> {
                if (log.isDebugEnabled()) {
                    log.debug(addFieldsOperation.toDocument(Aggregation.DEFAULT_CONTEXT).toJson());
                }
                aggregationOperations.add(operation);
            });
            Optional.ofNullable(searchCriteria)
                    .ifPresent(criteria -> aggregationOperations.add(Aggregation.match(criteria)));
            maxSizeBuilder.buildMaxSizes(mongoSearch).stream()
                    .forEach(m -> aggregationOperations.add((MatchOperation) m));

            Optional.ofNullable(sortBuilder.buildSortOperation(mongoSearch)).ifPresent(aggregationOperations::add);
            if (pagination != null) {
                aggregationOperations.add(Aggregation.skip(pagination.getPageNumber() * pagination.getPageSize()));
                aggregationOperations.add(Aggregation.limit(pagination.getPageSize()));
            }
            if (count) {
                aggregationOperations.add(Aggregation.count().as("total"));
            }
            return aggregationOperations;

        } catch (Exception ex) {
            throw new MongoSearchException(ex);
        }
    }

}
