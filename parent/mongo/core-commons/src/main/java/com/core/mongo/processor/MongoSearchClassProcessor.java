package com.core.mongo.processor;

import com.core.mongo.annotations.MongoSearchClass;
import com.core.mongo.components.CriteriaBuilderComponent;
import com.core.mongo.search.MongoSearch;
import com.core.mongo.utils.MethodInvoker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@Slf4j
@Qualifier("SEARCH_CLASS")
public class MongoSearchClassProcessor<S extends MongoSearch> implements MongoAnnotationProcessor<MongoAnnotationProcessorParameters<MongoSearchClass, S>> {

    @Autowired
    private CriteriaBuilderComponent builderComponent;

    @Override
    public void processAnnotation(
            MongoAnnotationProcessorParameters<MongoSearchClass, S> parameters) throws Exception {

        MongoSearchClass mongoSubSearchAnnotation = parameters.getAnnotation();
        Object object = MethodInvoker
                .invokeReadMethod(parameters.getBeanInfo(), parameters.getMongoSearch(), parameters.getField());
        if (object != null) {
            if (isFieldCollection(parameters.getField())) {
                Collection<Criteria> subCriterias = ((Collection<?>) object).stream().map(ele -> {
                    Criteria elementCriteria = null;
                    try {
                        S subSearch = (S) ele;
                        elementCriteria = processElement(mongoSubSearchAnnotation, subSearch, parameters.getField(),
                                parameters.getPath());
                    } catch (Exception ex) {
                        log.error("Error processing element of collecion subSearch " + ele.getClass().getName(), ex);
                    }
                    return elementCriteria;

                }).collect(Collectors.toSet());
                Criteria subCriteria = new Criteria();
                subCriteria.andOperator(subCriterias);
                addSubCriteriaToParams(mongoSubSearchAnnotation, subCriteria, parameters);

            } else {
                S subSearch = (S) object;
                Criteria subCriteria = processElement(mongoSubSearchAnnotation, subSearch, parameters.getField(),
                        parameters.getPath());
                addSubCriteriaToParams(mongoSubSearchAnnotation, subCriteria, parameters);
            }
        }
    }

    private void addSubCriteriaToParams(MongoSearchClass mongoSubSearchAnnotation, Criteria subCriteria,
                                        MongoAnnotationProcessorParameters parameters) {

        switch (mongoSubSearchAnnotation.conditionType()) {
            case AND:
                parameters.getAndColl().add(subCriteria);
                break;
            case OR:
                parameters.getOrColl().add(subCriteria);
            default:
        }
    }

    private Criteria processElement(MongoSearchClass mongoSubSearchAnnotation, S subSearch, Field field,
                                    String path) throws Exception {

        String propertyValue = mongoSubSearchAnnotation.value().isEmpty() ? field
                .getName() : mongoSubSearchAnnotation.value();
        String pathNext = StringUtils.isEmpty(propertyValue) ? "" :
                path + propertyValue + ".";

        Criteria subCriteria = builderComponent.buildCriteria(subSearch, new Criteria(),
                mongoSubSearchAnnotation.isCollection() ? "" : pathNext);
        if (mongoSubSearchAnnotation.isCollection()) {
            if (!mongoSubSearchAnnotation.not()) {
                subCriteria = Criteria.where(pathNext.substring(0, pathNext.length() - 1))
                        .elemMatch(subCriteria);
            } else {
                subCriteria = Criteria.where(pathNext.substring(0, pathNext.length() - 1)).not()
                        .elemMatch(subCriteria);
            }
        }
        return subCriteria;
    }

    public boolean isFieldCollection(Field field) {

        return Collection.class.isAssignableFrom(field.getType());
    }
}
