package com.common.view;

import com.core.view.View;
import com.core.view.annotations.FillerProperty;
import com.core.view.annotations.ViewProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class FillerResolver {

    private static Logger logger = Logger.getLogger(FillerResolver.class);

    public static <V extends View, E extends View, R extends View> void resolve(Collection<V> views,
                                                                                List<FillerFunction<? extends E, ? extends R>> fillerFunctions) throws Exception {

        fillerFunctions.stream().forEach(fillerFunction -> {
            resolveFunction(views, fillerFunction);
        });
    }

    private static <V extends View, E extends View, R extends View> void resolveFunction(Collection<V> views,
                                                                                         FillerFunction<E, R> fillerFunction) {

        List<Integer> fillerFunctionsIds = new ArrayList<>();
        Collection<MethodId> methodIds = new ArrayList<>();

        try {
            fillRecursive(views, fillerFunction, fillerFunctionsIds, methodIds, CollectionUtils.EMPTY_COLLECTION,
                    CollectionUtils.EMPTY_COLLECTION);
        } catch (Exception ex) {
            logger.error("Error fillingMap for fillerFunction " + fillerFunction.getFunctionName(), ex);
        }

        Collection<? extends R> viewsToCollection = fillerFunction.getFillerFunction().apply(fillerFunctionsIds);
        methodIds.stream().forEach(methodId -> {
            try {
                Collector<R, ?, Set<R>> collector =
                        methodId.collector != null ? methodId.collector : Collectors.toSet();
                Collection<R> viewsToCollectionFiltered = viewsToCollection.stream()
                        .filter(v -> fillerFunction.getResponseId().apply(v).equals(methodId.id))
                        .collect(collector);
                if (methodId.collector == null) {
                    methodId.method.invoke(methodId.view, viewsToCollectionFiltered.stream().findFirst().get());
                } else {
                    methodId.method.invoke(methodId.view, viewsToCollectionFiltered);
                }
            } catch (Exception ex) {
                logger.error("Error invoking " + methodId.method + " on view " + methodId.view, ex);
            }
        });
    }

    public static <V extends View, E extends View, R extends View> void fillRecursive(Collection<V> views,
                                                                                      FillerFunction<E, R> fillerFunction,
                                                                                      List<Integer> fillerFunctionIds,
                                                                                      Collection<MethodId> methodIds,
                                                                                      Collection<String> excludeProperties,
                                                                                      Collection<String> includedProperties) throws Exception {

        if (!CollectionUtils.emptyIfNull(views).isEmpty()) {
            Map<Class<? super V>, BeanInfo> beanInfoMap = new HashMap<>();
            views.removeIf(Objects::isNull);
            for (View view : views) {
                Class<? super V> clazzView = (Class) view.getClass();
                BeanInfo beanInfo = getBeanInfo(clazzView, beanInfoMap);
                while (clazzView != null) {
                    for (Field field : clazzView.getDeclaredFields()) {
                        if (field.isAnnotationPresent(FillerProperty.class)) {
                            FillerProperty fillerProperty = field.getAnnotation(FillerProperty.class);
                            if (fillerProperty.fillerFunctionName().equals(fillerFunction.getFunctionName())) {
                                resolveSupplier(fillerProperty, beanInfo, field, fillerFunction, view,
                                        fillerFunctionIds, methodIds);
                            }

                            Method readMethod = Arrays.stream(beanInfo.getPropertyDescriptors())
                                    .filter(propertyDescriptor -> propertyDescriptor.getName().equals(field.getName()))
                                    .map(PropertyDescriptor::getReadMethod).findFirst().orElseThrow(() -> new Exception(
                                            "No found readMethod for seacrhProperty " +
                                                    fillerProperty.fillerFunctionName() + " in view " +
                                                    view.getClass()));

                            Optional.ofNullable(readMethod.invoke(view)).ifPresent(toInvoke -> {
                                try {
                                    if (isFieldView(field)) {
                                        fillRecursive(Arrays.asList((V) toInvoke), fillerFunction, fillerFunctionIds,
                                                methodIds, CollectionUtils.EMPTY_COLLECTION,
                                                CollectionUtils.EMPTY_COLLECTION);
                                    } else if (isFieldCollectionOfViews(field)) {
                                        fillRecursive((Collection) toInvoke, fillerFunction, fillerFunctionIds,
                                                methodIds, CollectionUtils.EMPTY_COLLECTION,
                                                CollectionUtils.EMPTY_COLLECTION);
                                    }
                                } catch (Exception ex) {
                                    logger.error("Error fillingRecursive(fillerProperty) " +
                                            fillerProperty.fillerFunctionName() + " in view " + view.getClass() +
                                            " for field " + field);
                                }
                            });

                        } else if (field.isAnnotationPresent(ViewProperty.class)) {
                            ViewProperty viewProperty = field.getAnnotation(ViewProperty.class);
                            String propertyName = viewProperty.value().isEmpty() ? field.getName() : viewProperty.value();
                            if (filterProperty(propertyName, excludeProperties, includedProperties)) {
                                Method readMethod = Arrays.stream(beanInfo.getPropertyDescriptors())
                                        .filter(propertyDescriptor -> propertyDescriptor.getName()
                                                .equals(field.getName()))
                                        .map(PropertyDescriptor::getReadMethod).findFirst().orElseThrow(
                                                () -> new Exception(
                                                        "No found readMethod for viewProperty " + propertyName +
                                                                " in view " + view.getClass()));

                                Optional.ofNullable(readMethod.invoke(view)).ifPresent(toInvoke -> {
                                    try {
                                        if (isFieldView(field)) {
                                            fillRecursive(Arrays.asList((V) toInvoke), fillerFunction,
                                                    fillerFunctionIds, methodIds,
                                                    Arrays.asList(viewProperty.excludeProperties()),
                                                    Arrays.asList(viewProperty.includeProperties()));
                                        } else if (isFieldCollectionOfViews(field)) {
                                            fillRecursive((Collection) toInvoke, fillerFunction, fillerFunctionIds,
                                                    methodIds, Arrays.asList(viewProperty.excludeProperties()),
                                                    Arrays.asList(viewProperty.includeProperties()));
                                        }
                                    } catch (Exception ex) {
                                        logger.error("Error fillingRecursive (viewProperty) " + propertyName +
                                                " view " + view.getClass() + " for field " + field);
                                    }
                                });
                            }
                        }
                    }
                    clazzView = clazzView.getSuperclass();
                }
            }
        }
    }

    private static <V extends View, E extends View, R extends View> void resolveSupplier(FillerProperty fillerProperty,
                                                                                         BeanInfo beanInfo, Field field,
                                                                                         FillerFunction<E, R> fillerFunction,
                                                                                         V view,
                                                                                         List<Integer> fillerFunctionIds,
                                                                                         Collection<MethodId> methodIds) {

        fillerFunction.getSuppliersId().stream().filter(s -> fillerProperty.supplier().isEmpty() ||
                s.getSupplierName().equals(fillerProperty.supplier())).forEach(supplierId -> {
            try {
                Integer id = Optional.ofNullable(supplierId).map(supplier ->
                        supplier.getSupplierFunction().apply((E) view)).orElse(null);

                if (id != null) {

                    if (!fillerFunctionIds.contains(id)) {
                        fillerFunctionIds.add(id);
                    }
                    Method writeMethod = Arrays.stream(beanInfo.getPropertyDescriptors())
                            .filter(propertyDescriptor -> propertyDescriptor.getName().equals(field.getName()))
                            .map(PropertyDescriptor::getWriteMethod).findFirst().orElseThrow(() -> new Exception(
                                    "No found writeMethod for fillProperty " + fillerProperty.fillerFunctionName() +
                                            " in view " + view.getClass()));

                    Collector collector = null;
                    if (isFieldCollectionOfViews(field)) {
                        if (Set.class.isAssignableFrom(field.getType())) {
                            collector = Collectors.toSet();
                        } else if (List.class.isAssignableFrom(field.getType())) {
                            collector = Collectors.toList();
                        } else {
                            throw new Exception("No found collector for class " + field.getType());
                        }
                    }

                    MethodId methodId = new MethodId(writeMethod, id, view, collector);
                    methodIds.add(methodId);
                }

            } catch (Exception ex) {
                logger.error(
                        "Error resolviong fillerFunction " + fillerProperty.fillerFunctionName() + " for clazzView " +
                                view.getClass());
            }
        });
    }

    private static boolean filterProperty(String propertyValue, Collection<String> excludedProperties,
                                          Collection<String> includedProperties) {

        if (Optional.ofNullable(includedProperties)
                .map(prop -> prop.isEmpty() || (!prop.isEmpty() && prop.contains(propertyValue)))
                .orElse(Boolean.TRUE)) {
            return !excludedProperties.contains(propertyValue);
        } else {
            return Boolean.FALSE;
        }
    }

    public static boolean isFieldView(Field field) {

        boolean isFieldView = false;
        if (View.class.isAssignableFrom(findSuperClass(field.getType())) && !findSuperClass(field.getType())
                .isAssignableFrom(Object.class)) {
            isFieldView = true;
        }
        return isFieldView;
    }

    public static boolean isFieldCollectionOfViews(Field field) {

        boolean isFieldCollectionOfViews = false;
        if (Collection.class.isAssignableFrom(field.getType())) {
            Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (View.class.isAssignableFrom(findSuperClass((Class) type))) {
                isFieldCollectionOfViews = true;
            } else {
                isFieldCollectionOfViews = false;
            }
        }
        return isFieldCollectionOfViews;
    }

    public static Class findSuperClass(Class clazz) {

        if (clazz.getSuperclass() == null || clazz.getSuperclass().isAssignableFrom(Object.class)) {
            return clazz;
        } else {
            return findSuperClass(clazz.getSuperclass());
        }
    }

    private static <V extends View> BeanInfo
    getBeanInfo(Class<? super V> clazzView, Map<Class<? super V>, BeanInfo> beasnInfoMap)
            throws Exception {

        BeanInfo beanInfo = beasnInfoMap.get(clazzView);
        if (beanInfo == null) {
            beanInfo = Introspector.getBeanInfo(clazzView);
            beasnInfoMap.put(clazzView, beanInfo);
        }
        return beanInfo;
    }

    static class MethodId<V extends View> {

        private final Method method;
        private final Integer id;
        private final V view;
        private final Collector collector;

        public MethodId(Method method, Integer id, V view, Collector collector) {

            this.method = method;
            this.id = id;
            this.view = view;
            this.collector = collector;
        }
    }

}
