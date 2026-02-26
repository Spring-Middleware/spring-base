package com.middleware.mongo.components;

import com.middleware.mongo.annotations.MongoAddFieldProperty;
import com.middleware.mongo.annotations.MongoSearchClass;
import com.middleware.mongo.resolver.MongoAggregationPropertyResolver;
import com.middleware.mongo.search.MongoSearch;
import com.middleware.mongo.utils.MethodInvoker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

@Slf4j
@Component
public class AddFieldsBuilderComponent<S extends MongoSearch> {

    public AddFieldsOperation buildAddFields(S mongoSearch) throws Exception {

        AddFieldsOperation addFieldsOperation = null;
        AddFieldsOperation.AddFieldsOperationBuilder builder = AddFieldsOperation.builder();
        if (processAddFields(mongoSearch, builder, "", false, false)) {
            addFieldsOperation = builder.build();
        }
        return addFieldsOperation;
    }

    public Mono<AddFieldsOperation> buildAddFieldsAsync(Mono<S> mongoSearch) {

        return mongoSearch.flatMap(s -> {
            AddFieldsOperation addFieldsOperation = null;
            try {
                addFieldsOperation =buildAddFields(s);
            } catch (Exception ex) {
                log.error("Error building addFields", ex);
            }
            return Mono.justOrEmpty(addFieldsOperation);
        });
    }

    private boolean processAddFields(S mongoSearch, AddFieldsOperation.AddFieldsOperationBuilder builder,
                                     String path, boolean isCollection, boolean hasFields) throws Exception {

        BeanInfo beanInfo = null;
        Class<? super MongoSearch> clazzSearch = (Class) mongoSearch.getClass();
        while (clazzSearch != null) {
            beanInfo = Introspector.getBeanInfo(clazzSearch);
            for (Field field : clazzSearch.getDeclaredFields()) {
                Annotation aggregationProperty = getAnnotatedMongoAggregationProperty(field);
                if (aggregationProperty != null) {
                    MongoAddFieldProperty mongoAddFieldProperty = aggregationProperty.annotationType()
                            .getAnnotation(MongoAddFieldProperty.class);
                    MongoAggregationPropertyResolver<Annotation, S> aggregationPropertyResolver = (MongoAggregationPropertyResolver) Class
                            .forName(mongoAddFieldProperty
                                    .resolverClazz()).newInstance();
                    hasFields = aggregationPropertyResolver
                            .addFieldOperation(aggregationProperty, mongoSearch, builder, field, beanInfo, isCollection,
                                    path);
                } else if (field.isAnnotationPresent(MongoSearchClass.class)) {
                    MongoSearchClass mongoSubSearchAnnotation = field
                            .getAnnotation(MongoSearchClass.class);
                    MongoSearch subSearch = MethodInvoker.invokeReadMethod(beanInfo, mongoSearch, field);
                    if (subSearch != null) {
                        String propertyValue = mongoSubSearchAnnotation.value().isEmpty() ? field
                                .getName() : mongoSubSearchAnnotation.value();
                        String pathNext = StringUtils.isEmpty(propertyValue) ? "" :
                                path + propertyValue + ".";
                        hasFields = hasFields || processAddFields((S) subSearch, builder, pathNext,
                                mongoSubSearchAnnotation.isCollection(), hasFields);
                    }
                }
            }
            clazzSearch = clazzSearch.getSuperclass();
        }
        return hasFields;
    }

    private Annotation getAnnotatedMongoAggregationProperty(Field field) {

        return Arrays.stream(field.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(
                MongoAddFieldProperty.class)).findFirst().orElse(null);
    }

}
