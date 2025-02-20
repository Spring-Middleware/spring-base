package com.core.utils;

import com.core.view.DataAdaptor;
import com.core.view.annotations.DataAdapter;
import org.apache.commons.lang3.StringUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class ReflectionUtils {

    public static String getPropertyNameForField(Field field) {

        if ((field.getType().isAssignableFrom(boolean.class) || field.getType()
                .isAssignableFrom(Boolean.class)) && field.getName().startsWith("is")) {
            return StringUtils.uncapitalize(field.getName().substring(2));
        }
        return field.getName();
    }

    public static Class findSuperClass(Class clazz) {

        if (clazz.getSuperclass() == null || clazz.getSuperclass().isAssignableFrom(Object.class)) {
            return clazz;
        } else {
            return findSuperClass(clazz.getSuperclass());
        }
    }

    public static Method getWriteMethodForField(BeanInfo beanInfo, Field field) {

        return Arrays.stream(
                        beanInfo.getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getName()
                        .equals(ReflectionUtils.getPropertyNameForField(field)))
                .map(PropertyDescriptor::getWriteMethod).findFirst().orElse(null);
    }

    public static Method getReadMethodForField(BeanInfo beanInfo, Field field) {

        return Arrays.stream(
                        beanInfo.getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getName()
                        .equals(ReflectionUtils.getPropertyNameForField(field)))
                .map(PropertyDescriptor::getReadMethod).findFirst().orElse(null);
    }

    public static <E, S> S adaptIfNecessary(E e, Method method) throws Exception {

        if (method.isAnnotationPresent(DataAdapter.class)) {
            DataAdapter dataAdapter = method.getAnnotation(DataAdapter.class);
            DataAdaptor dataAdaptor = dataAdapter.value().newInstance();
            return (S) dataAdaptor.adapt(e);
        } else {
            return (S) e;
        }
    }

    public static <T> BeanInfo getBeanInfo(Class<? super T> clazzT,
            Map<Class<? super T>, BeanInfo> beasnInfoMap)
            throws Exception {

        BeanInfo beanInfo = beasnInfoMap.get(clazzT);
        if (beanInfo == null) {
            beanInfo = Introspector.getBeanInfo(clazzT);
            beasnInfoMap.put(clazzT, beanInfo);
        }
        return beanInfo;
    }

}
