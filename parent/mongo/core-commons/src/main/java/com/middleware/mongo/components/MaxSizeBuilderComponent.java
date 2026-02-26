package com.middleware.mongo.components;

import com.middleware.mongo.annotations.MongoMaxSize;
import com.middleware.mongo.search.MongoSearch;
import com.middleware.mongo.utils.MethodInvoker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;

@Slf4j
@Component
public class MaxSizeBuilderComponent<S extends MongoSearch> {

    public Collection<MatchOperation> buildMaxSizes(S mongoSearch) throws Exception {

        BeanInfo beanInfo = null;
        Class<? super MongoSearch> clazzSearch = (Class) mongoSearch.getClass();
        Collection<MatchOperation> matchOperations = new HashSet<>();
        while (clazzSearch != null) {
            beanInfo = Introspector.getBeanInfo(clazzSearch);
            for (Field field : clazzSearch.getDeclaredFields()) {
                if (field.isAnnotationPresent(MongoMaxSize.class)) {
                    MongoMaxSize mongoMaxSize = field.getAnnotation(MongoMaxSize.class);
                    Integer max = (Integer) MethodInvoker
                            .invokeReadMethod(beanInfo, mongoSearch, field);
                    if (max != null) {
                        Criteria maxCriteria = new Criteria(mongoMaxSize.value() + "." + (max-1));
                        matchOperations.add(Aggregation.match(maxCriteria.not().exists(true)));
                    }
                }
            }
            clazzSearch = clazzSearch.getSuperclass();
        }
        return matchOperations;
    }
}