package com.middleware.mongo.utils;


import com.middleware.mongo.search.MongoSearch;

import java.beans.BeanInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MethodInvoker {

    public static <S extends MongoSearch, R> R invokeReadMethod(BeanInfo beanInfo, S search, Field field) {

        try {
            if (beanInfo != null) {
                return (R) Arrays.stream(beanInfo.getPropertyDescriptors())
                        .filter(propertyDescriptor -> propertyDescriptor.getName().equals(field.getName()))
                        .map(propertyDescriptor -> invokeReadMethod(propertyDescriptor.getReadMethod(), search))
                        .collect(Collectors.toList()).get(0);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static <R> R invokeReadMethod(Method readMethod, MongoSearch search) {

        try {
            return (R) readMethod.invoke(search);
        } catch (Exception e) {
            return null;
        }
    }

}
