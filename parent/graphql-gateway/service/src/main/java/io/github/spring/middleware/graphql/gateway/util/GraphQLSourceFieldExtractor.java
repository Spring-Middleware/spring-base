package io.github.spring.middleware.graphql.gateway.util;

import io.github.spring.middleware.graphql.gateway.exception.GraphQLErrorCodes;
import io.github.spring.middleware.graphql.gateway.exception.GraphQLException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;


public class GraphQLSourceFieldExtractor {

    public static Object extractFieldValue(Object source, String fieldName) {
        if (source == null) {
            return null;
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new GraphQLException(GraphQLErrorCodes.FIELD_EXTRACTION_ERROR, "Field name must not be null or blank");
        }

        if (source instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }

        Object value = extractFromBean(source, fieldName);
        if (value != FIELD_NOT_FOUND) {
            return value;
        }

        throw new GraphQLException(GraphQLErrorCodes.FIELD_EXTRACTION_ERROR,
                "Unable to extract field '%s' from source of type '%s'"
                        .formatted(fieldName, source.getClass().getName())
        );
    }

    private static Object extractFromBean(Object source, String fieldName) {
        try {
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(source.getClass()).getPropertyDescriptors()) {
                if (fieldName.equals(propertyDescriptor.getName()) && propertyDescriptor.getReadMethod() != null) {
                    return propertyDescriptor.getReadMethod().invoke(source);
                }
            }
            return FIELD_NOT_FOUND;
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException ex) {
            throw new GraphQLException(GraphQLErrorCodes.FIELD_EXTRACTION_ERROR,
                    "Error extracting field '%s' from source of type '%s'"
                            .formatted(fieldName, source.getClass().getName()),
                    ex
            );
        }
    }

    private static final Object FIELD_NOT_FOUND = new Object();
}