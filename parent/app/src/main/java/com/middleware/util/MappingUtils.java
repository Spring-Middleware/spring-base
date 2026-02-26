package com.middleware.util;

import com.middleware.annotation.MappedClass;
import com.middleware.annotation.MappedContextParam;
import com.middleware.annotation.MappedParam;
import com.middleware.filter.Context;
import com.middleware.utils.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import reactor.util.context.ContextView;

import java.beans.BeanInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MappingUtils {

    private static Logger log = Logger.getLogger(MappingUtils.class);

    public static <T> T map(Map<String, String[]> params, Class<? super T> clazz,
            ContextView context) throws Exception {

        return map(params, clazz, context, null);
    }

    public static <T> T map(Map<String, String[]> params, Class<? super T> clazz) throws Exception {

        return map(params, clazz, null, null);
    }

    public static <T> T map(Map<String, String[]> params, Class<? super T> clazz,
            Class<?>... generalizedType) throws Exception {

        return map(params, clazz, null, generalizedType);
    }

    public static <T> T map(Map<String, String[]> params, Class<? super T> clazz, ContextView context,
            Class<?>... generalizedType) throws Exception {

        T mapping = (T) clazz.getDeclaredConstructor().newInstance();
        Map<Class<? super T>, BeanInfo> beanInfoMap = new HashMap<>();
        while (clazz != null) {
            BeanInfo beanInfo = ReflectionUtils.getBeanInfo(clazz, beanInfoMap);
            mapParams(clazz, mapping, beanInfo, params);
            mapContextParams(clazz, mapping, beanInfo, context);
            mapClasses(clazz, mapping, beanInfo, params, context, generalizedType);
            clazz = clazz.getSuperclass();
        }
        return mapping;
    }

    private static <T> void mapContextParams(Class<? super T> clazz, T mapping, BeanInfo beanInfo,
            ContextView context) {

        String clazzName = clazz.getName();
        Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(MappedContextParam.class))
                .forEach(field -> {
                    MappedContextParam mappedContextParam = field.getAnnotation(MappedContextParam.class);
                    Method setMethod = ReflectionUtils.getWriteMethodForField(beanInfo, field);
                    try {
                        setMethod.invoke(mapping,
                                getObjectType(field.getType(), getFromContext(context, mappedContextParam), setMethod));
                    } catch (Exception ex) {
                        log.error("Error invoking method " + setMethod.getName() + " in clazz " +
                                clazzName + " for contextProperty " + mappedContextParam.value(), ex);
                    }
                });
    }

    private static <T> T getFromContext(ContextView context, MappedContextParam mappedContextParam) {

        return (T) Optional.ofNullable(context).filter(c -> c.hasKey(mappedContextParam.value()))
                .map(c -> c.get(mappedContextParam.value()))
                .orElseGet(() -> Context.get(mappedContextParam.value()));
    }

    private static <T> void mapParams(Class<? super T> clazz, T mapping, BeanInfo beanInfo,
            Map<String, String[]> params) {

        String clazzName = clazz.getName();
        Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(MappedParam.class))
                .forEach(field -> {
                    MappedParam mappedParam = field.getAnnotation(MappedParam.class);
                    String paramName = mappedParam.value()
                            .equals(StringUtils.EMPTY) ? field.getName() : mappedParam.value();
                    Optional.ofNullable(params.get(paramName)).ifPresent(paramValues -> {
                        Method setMethod = ReflectionUtils.getWriteMethodForField(beanInfo, field);
                        if (setMethod != null) {
                            try {
                                if (Collection.class.isAssignableFrom(field.getType())) {
                                    Collection collection = getProperCollection(field);
                                    ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                                    Class<?> clazzType = (Class) parameterizedType.getActualTypeArguments()[0];
                                    for (int i = 0; i < paramValues.length; i++) {
                                        collection.add(getObjectType(clazzType, paramValues[i], setMethod));
                                    }
                                    setMethod.invoke(mapping, collection);

                                } else {
                                    setMethod.invoke(mapping,
                                            getObjectType(field.getType(), paramValues[0], setMethod));
                                }

                            } catch (Exception ex) {
                                log.error("Error invoking method " + setMethod.getName() + " in clazz " +
                                        clazzName, ex);
                            }
                        }
                    });
                });
    }

    private static Collection getProperCollection(Field field) throws Exception {

        Collection collection = null;
        if (Collection.class.isAssignableFrom(field.getType()) && !field.getType().isInterface()) {
            collection = (Collection) field.getType().getDeclaredConstructor().newInstance();
        } else {
            if (List.class.isAssignableFrom(field.getType())) {
                collection = new ArrayList();
            } else if (Set.class.isAssignableFrom(field.getType())) {
                collection = new HashSet();
            }
        }
        return collection;
    }

    private static Object getObjectType(Class<?> clazzType, String value, Method setMethod) throws Exception {

        Object object = null;
        if (!clazzType.isEnum()) {
            if (Date.class.isAssignableFrom(clazzType)) {
                object = Date.valueOf(LocalDate.parse(value));
            } else if (LocalDate.class.isAssignableFrom(clazzType)) {
                object = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            } else if (Timestamp.class.isAssignableFrom(clazzType)) {
                object = Timestamp.valueOf(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME));
            } else if (LocalDateTime.class.isAssignableFrom(clazzType)) {
                object = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (LocalTime.class.isAssignableFrom(clazzType)) {
                object = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME);
            } else if (Integer.class.isAssignableFrom(clazzType) || int.class.isAssignableFrom(clazzType)) {
                object = Integer.parseInt(value);
            } else if (Double.class.isAssignableFrom(clazzType) || double.class.isAssignableFrom(clazzType)) {
                object = Double.valueOf(value);
            } else if (UUID.class.isAssignableFrom(clazzType)) {
                object = UUID.fromString(value);
            } else {
                object = ReflectionUtils.adaptIfNecessary(value, setMethod);
            }
        } else {
            object = Enum.valueOf((Class) clazzType, value);
        }
        return object;
    }

    private static <T> void mapClasses(Class<? super T> clazz, T mapping, BeanInfo beanInfo,
            Map<String, String[]> params, ContextView context, Class<?>... generalizedType) {

        Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(MappedClass.class))
                .forEach(field -> {
                    MappedClass mappedClass = field.getAnnotation(MappedClass.class);
                    Collection<String> subParams = Arrays.asList(mappedClass.params());
                    Map<String, String[]> subParamsMap = params.entrySet().stream()
                            .filter(e -> subParams.isEmpty() || subParams.contains(e.getKey()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, Map.Entry::getValue));

                    if (!subParamsMap.isEmpty()) {
                        Method setMethod = ReflectionUtils.getWriteMethodForField(beanInfo, field);
                        try {
                            if (!(field.getGenericType() instanceof TypeVariable)) {
                                setMethod.invoke(mapping,
                                        MappingUtils.map(subParamsMap, (Class) field.getType(), context,
                                                generalizedType));
                            } else {
                                TypeVariable typeVariable = (TypeVariable) field.getGenericType();
                                Class<?> bounded = (Class) typeVariable.getBounds()[0];
                                Type subType = null;
                                if (generalizedType != null) {
                                    subType = Arrays.stream(generalizedType)
                                            .filter(gt -> bounded.isAssignableFrom(gt))
                                            .findFirst()
                                            .orElse(null);
                                }
                                if (subType != null) {
                                    setMethod.invoke(mapping,
                                            MappingUtils.map(subParamsMap, (Class) subType, context,
                                                    generalizedType));
                                }
                            }
                        } catch (Exception ex) {
                            log.error("Error invoking setMethod for mappedClass " + field.getName() + " class " +
                                    field.getClass().getName(), ex);
                        }
                    }
                });
    }

}
