package com.core.mongo.resolver;

import com.core.mongo.search.MongoSearch;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;

import java.beans.BeanInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface MongoAggregationPropertyResolver<A extends Annotation, S extends MongoSearch> {

    boolean addFieldOperation(A annotation, S mongoSearch, AddFieldsOperation.AddFieldsOperationBuilder builder,
                           Field field,
                           BeanInfo beanInfo, boolean isCollection, String path);
}
