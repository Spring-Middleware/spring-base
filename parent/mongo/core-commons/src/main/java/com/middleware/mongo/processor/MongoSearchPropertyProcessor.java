package com.middleware.mongo.processor;

import com.middleware.mongo.annotations.MongoSearchProperty;
import com.middleware.mongo.function.FunctionsOperationType;
import com.middleware.mongo.search.MongoSearch;
import com.middleware.mongo.utils.MethodInvoker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Qualifier("SEARCH_PROPERTY")
public class MongoSearchPropertyProcessor<S extends MongoSearch> implements MongoAnnotationProcessor<MongoAnnotationProcessorParameters<MongoSearchProperty, S>> {

    @Override
    public void processAnnotation(
            MongoAnnotationProcessorParameters<MongoSearchProperty, S> parameters) throws Exception {

        MongoSearchProperty mongoSearchProperty = parameters.getAnnotation();
        Object value = MethodInvoker
                .invokeReadMethod(parameters.getBeanInfo(), parameters.getMongoSearch(), parameters.getField());
        if (value != null) {
            if (value instanceof ZonedDateTime) {
                ZonedDateTime zonedDateTime = (ZonedDateTime) value;
                value = zonedDateTime.toLocalDateTime();
            }
            Collection<String> propertiesValue = mongoSearchProperty.value().isEmpty() ? Arrays
                    .asList(parameters.getField()
                            .getName()) : formatPropertyValueParameters(mongoSearchProperty.value(),
                    parameters.getMongoSearch(),
                    parameters.getBeanInfo());

            Criteria criteriaField = getCriteriaField(propertiesValue, value, mongoSearchProperty,
                    parameters.getPath());
            switch (mongoSearchProperty.conditionType()) {
                case AND:
                    parameters.getAndColl().add(criteriaField);
                    break;
                case OR:
                    parameters.getOrColl().add(criteriaField);
                default:
            }
        }

    }

    private static Criteria getCriteriaField(Collection<String> propertiesValue, Object value,
                                             MongoSearchProperty mongoSearchProperty, String path) {

        Criteria criteriaField;
        if (propertiesValue.size() > 1) {
            criteriaField = new Criteria();
            criteriaField.orOperator(propertiesValue.stream().map(propertyValue -> {
                return FunctionsOperationType
                        .applyOperation(mongoSearchProperty.operationType(),
                                Criteria.where(path + propertyValue), value);
            }).collect(Collectors.toSet()));
        } else {
            criteriaField = FunctionsOperationType
                    .applyOperation(mongoSearchProperty.operationType(),
                            Criteria.where(path + propertiesValue.stream().findFirst().get()),
                            value);
        }
        return criteriaField;
    }

    private static <S extends MongoSearch> Collection<String> formatPropertyValueParameters(String propertyValue,
                                                                                            S mongoSearch,
                                                                                            BeanInfo beanInfo) throws Exception {

        Collection<String> result = new HashSet<>();
        String[] parts = propertyValue.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("$")) {
                String nameField = parts[i].substring(parts[i].indexOf("$") + 1);
                PropertyDescriptor propertyDescriptor = Arrays.stream(beanInfo.getPropertyDescriptors())
                        .filter(p -> p.getName().equals(nameField)).findFirst()
                        .orElse(null);

                Object substituteObject = Optional.ofNullable(propertyDescriptor)
                        .map(pd -> MethodInvoker.invokeReadMethod(propertyDescriptor.getReadMethod(), mongoSearch))
                        .orElse(null);

                int j = i;
                if (Collection.class.isInstance(substituteObject)) {
                    Collection<String> substitutesValues = (Collection) substituteObject;
                    String property = propertyValue;
                    result = substitutesValues.stream().filter(Objects::nonNull)
                            .map(s -> property.replace(parts[j], s)).collect(Collectors.toSet());
                } else {
                    result.add(propertyValue
                            .replace(parts[i],
                                    (String) Optional.ofNullable(substituteObject).orElse(StringUtils.EMPTY)));
                }
            }
        }
        if (result.isEmpty())
            result.add(propertyValue);
        return result;
    }

}
