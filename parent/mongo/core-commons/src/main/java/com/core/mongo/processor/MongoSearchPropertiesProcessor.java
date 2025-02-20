package com.core.mongo.processor;

import com.core.mongo.annotations.MongoSearchProperties;
import com.core.mongo.annotations.MongoSearchProperty;
import com.core.mongo.search.MongoSearch;
import com.core.mongo.types.ConditionType;
import com.core.mongo.utils.MethodInvoker;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@Service
@Qualifier("SEARCH_PROPERTIES")
public class MongoSearchPropertiesProcessor<S extends MongoSearch> implements MongoAnnotationProcessor<MongoAnnotationProcessorParameters<MongoSearchProperties, S>> {

    @Autowired
    @Qualifier("SEARCH_PROPERTY")
    private MongoAnnotationProcessor searchPropertyProcessor;

    @Override
    public void processAnnotation(
            MongoAnnotationProcessorParameters parameters) throws Exception {

        Object value = MethodInvoker
                .invokeReadMethod(parameters.getBeanInfo(), parameters.getMongoSearch(), parameters.getField());

        if (value != null) {

            MongoSearchProperties mongoSearchProperties = parameters.getField()
                    .getAnnotation(MongoSearchProperties.class);
            Set<Criteria> andColl = new HashSet<>();
            Set<Criteria> orColl = new HashSet<>();

            MongoAnnotationProcessorParameters.MongoAnnotationProcessorParametersBuilder<MongoSearchProperty, S> builder
                    = (MongoAnnotationProcessorParameters.MongoAnnotationProcessorParametersBuilder)
                    MongoAnnotationProcessorParameters.builder()
                            .field(parameters.getField())
                            .mongoSearch(parameters.getMongoSearch())
                            .beanInfo(parameters.getBeanInfo())
                            .path(parameters.getPath())
                            .andColl(andColl)
                            .orColl(orColl);

            Arrays.stream(mongoSearchProperties.value()).forEach(mongoSearchProperty -> {
                try {
                    builder.annotation(mongoSearchProperty);
                    searchPropertyProcessor.processAnnotation(builder.build());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            Criteria propertiesCriteria = new Criteria();
            Collection<Criteria> ors = new HashSet<>();
            Optional.ofNullable(buildOperation(andColl, Criteria::andOperator))
                    .ifPresent(ors::add);
            Optional.ofNullable(buildOperation(orColl, Criteria::orOperator))
                    .ifPresent(ors::add);
            propertiesCriteria.orOperator(ors);

            if (mongoSearchProperties.conditionType() == ConditionType.AND) {
                parameters.getAndColl().add(propertiesCriteria);
            } else if (mongoSearchProperties.conditionType() == ConditionType.OR) {
                parameters.getOrColl().add(propertiesCriteria);
            }
        }

    }

    private Criteria buildOperation(Collection<Criteria> criterias,
                                    BiFunction<Criteria, Collection<Criteria>, Criteria> applyFunction) {

        Criteria criteria = null;
        if (!CollectionUtils.emptyIfNull(criterias).isEmpty()) {
            criteria = new Criteria();
            applyFunction.apply(criteria, criterias);
        }
        return criteria;
    }

}
