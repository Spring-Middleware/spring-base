package com.core.mongo.resolver;

import com.core.mongo.annotations.MongoClazzRef;
import com.core.mongo.annotations.MongoConcatProperty;
import com.core.mongo.search.MongoSearch;
import com.core.mongo.utils.MethodInvoker;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
import org.springframework.data.mongodb.core.aggregation.VariableOperators;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MongoConcatPropertyResolver<S extends MongoSearch> implements MongoAggregationPropertyResolver<MongoConcatProperty, S> {

    @Override
    public boolean addFieldOperation(MongoConcatProperty mongoConcatProperty, S mongoSearch,
                                     AddFieldsOperation.AddFieldsOperationBuilder builder,
                                     Field field, BeanInfo beanInfo, boolean isCollection, String path) {

        Object value = MethodInvoker.invokeReadMethod(beanInfo, mongoSearch, field);
        boolean added = false;
        if (value != null) {
            added = true;
            if (!isCollection) {
                StringOperators.Concat concat = StringOperators.Concat.valueOf(path + mongoConcatProperty.value());
                for (int i = 0; i < mongoConcatProperty.concat().length; i++) {
                    concat = concat.concat(mongoConcatProperty.separator());
                    concat = concat.concatValueOf(path + mongoConcatProperty.concat()[i]);
                }
                builder.addFieldWithValue(path + field.getName(), concat);

            } else {
                VariableOperators.Map map = VariableOperators.mapItemsOf(path.substring(0, path.length() - 1)).as("u")
                        .andApply(aggregationOperationContext -> {
                            Document document = new Document(field.getName(),
                                    new Document("$concat", getConcatPropertiesForArray(mongoConcatProperty)));
                            getPropertiesClazzRef(mongoSearch).stream().forEach(prop -> {
                                document.append(parseToPropertyIdName(prop), "$$u." + parseToPropertyIdName(prop));
                            });
                            return document;
                        });
                builder.addFieldWithValue(path.substring(0, path.length() - 1), map);
            }
        }
        return added;
    }

    private String parseToPropertyIdName(String property) {
        return property.equals("id") ? "_id" : property;
    }

    private Collection<String> getPropertiesClazzRef(S mongoSearch) {

        Collection<String> properties = null;
        try {
            if (mongoSearch.getClass().isAnnotationPresent(MongoClazzRef.class)) {
                MongoClazzRef mongoClazzRef = mongoSearch.getClass().getAnnotation(MongoClazzRef.class);
                BeanInfo beanInfo = Introspector.getBeanInfo(Class.forName(mongoClazzRef.value()));
                properties = Arrays.stream(beanInfo.getPropertyDescriptors()).map(p -> p.getName())
                        .collect(Collectors.toSet());

            }
        } catch (Exception ex) {
            log.error("Error retriving class for class " + mongoSearch.getClass().getName(), ex);
        }
        return properties;
    }

    private List<String> getConcatPropertiesForArray(MongoConcatProperty mongoConcatProperty) {

        List<String> concatProperties = new ArrayList<>();
        concatProperties.add("$$u." + mongoConcatProperty.value());
        Arrays.stream(mongoConcatProperty.concat()).forEach(p -> {
            concatProperties.add(mongoConcatProperty.separator());
            concatProperties.add("$$u." + p);
        });
        return concatProperties;
    }

}

