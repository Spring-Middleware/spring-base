package com.common.view;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.middleware.utils.ReflectionUtils;
import com.middleware.view.ContextFilterPredicate;
import com.middleware.view.PropertyRolesAllowedAuthorizer;
import com.middleware.view.View;
import com.middleware.view.annotations.PropertyRolesAllowed;
import com.middleware.view.annotations.ViewFromClass;
import com.middleware.view.annotations.ViewProperty;
import com.middleware.view.annotations.ViewType;
import org.apache.log4j.Logger;
import org.hibernate.LazyInitializationException;
import reactor.util.context.ContextView;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ViewResolver {

    private static Logger logger = Logger.getLogger(ViewResolver.class);

    public static <V extends View, E> V resolveEntity(Class<V> clazzView, E entity) throws Exception {

        return (V) resolveEntityFiltering(Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, clazzView, entity);
    }

    public static <V extends View, E> V resolveEntity(Class<V> clazzView, E entity,
            ContextView contextView) throws Exception {

        return (V) resolveEntityFiltering(Collections.EMPTY_LIST, Collections.EMPTY_LIST, contextView, clazzView,
                entity);
    }

    public static <V extends View, E> V resolveEntityFiltering(Collection<String> excludeProperties,
            Collection<String> includedProperties,
            ContextView contextView,
            Class<V> clazzView,
            E entity) throws Exception {

        if (entity != null) {
            List<V> views = resolveFiltering(excludeProperties, includedProperties, clazzView, contextView, entity);
            if (views != null && !views.isEmpty()) {
                return views.iterator().next();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <V extends View, E> List<V> resolve(Class<? super V> clazzView, ContextView contextView,
            E... entities) throws Exception {

        return resolveFiltering(Collections.EMPTY_LIST, Collections.EMPTY_LIST, clazzView, contextView, entities);
    }

    @SuppressWarnings("unchecked")
    public static <V extends View, E> List<V> resolve(Class<? super V> clazzView,
            E... entities) throws Exception {

        return resolveFiltering(Collections.EMPTY_LIST, Collections.EMPTY_LIST, clazzView, null, entities);
    }

    public static <V extends View, E> List<V> resolveFiltering(Collection<String> excludeProperties,
            Collection<String> includedProperties,
            Class<? super V> clazzView,
            ContextView contextView,
            E... entities) throws Exception {

        List<V> views = null;
        Map<Class<? super V>, BeanInfo> beanInfoMap = new HashMap<>();
        if (entities != null && entities.length > 0) {
            views = new ArrayList<>();
            for (E entity : entities) {
                Class<? super V> primaryClassView = clazzView;
                clazzView = getClazzViewFromTypes(clazzView, entity);
                BeanInfo beanInfo = ReflectionUtils.getBeanInfo(clazzView, beanInfoMap);
                entity = HibernateUtils.unproxy(entity);
                V view = (V) clazzView.newInstance();
                while (clazzView != null) {
                    for (Field field : clazzView.getDeclaredFields()) {
                        if (field.isAnnotationPresent(ViewProperty.class)) {
                            ViewProperty viewProperty = field.getAnnotation(ViewProperty.class);
                            if (isAllowed(field) && filterProperty(viewProperty, field, excludeProperties,
                                    includedProperties)) {
                                String propertyName = viewProperty.value().isEmpty() ? field.getName() : viewProperty
                                        .value();
                                invokeSetMethod(beanInfo, view, field,
                                        getFinalEntityPath(entity, propertyName), viewProperty, contextView);
                            }
                        }
                    }
                    clazzView = clazzView.getSuperclass();
                }
                views.add(view);
                clazzView = primaryClassView;
            }
        }
        return views;
    }

    private static <V extends View> Class<V> getClazzViewFromTypes(Class<?> clazzView, Object entity) throws Exception {

        return Optional.ofNullable(getClazzViewFromTypesRecursive(clazzView, entity)).orElse((Class) clazzView);
    }

    private static <V extends View> Class<V> getClazzViewFromTypesRecursive(Class<?> clazzView,
            Object entity) throws Exception {

        ViewFromClass viewFromClass = clazzView.getAnnotation(ViewFromClass.class);
        Class<V> clazzViewFound = null;
        if (viewFromClass != null) {
            for (ViewType viewType : viewFromClass.types()) {
                if (HibernateUtils.isInstance(Class.forName(viewType.clazzName()), entity)) {
                    clazzViewFound = (Class) viewType.clazzView();
                } else {
                    clazzViewFound = getClazzViewFromTypesRecursive(viewType.clazzView(), entity);
                }
                if (clazzViewFound != null) {
                    return clazzViewFound;
                }
            }
        }
        return clazzViewFound;
    }

    /**
     * @param viewProperty
     * @param includedProperties
     * @param excludedProperties
     * @return true is viewProperty.values has to be processed
     */
    private static boolean filterProperty(ViewProperty viewProperty, Field field, Collection<String> excludedProperties,
            Collection<String> includedProperties) {

        String propertyName = viewProperty.value().isEmpty() ? field.getName() : viewProperty.value();
        if (Optional.ofNullable(includedProperties)
                .map(prop -> prop.isEmpty() || (!prop.isEmpty() && prop.contains(propertyName)))
                .orElse(Boolean.TRUE)) {
            return !excludedProperties.contains(propertyName);
        } else {
            return Boolean.FALSE;
        }
    }

    private static List<String> includePropertiesFilter(ViewProperty viewProperty) {

        try {
            Class<? extends Supplier> supplierClazz = viewProperty.applyIncludeProperties();
            Supplier<Boolean> mySupplier = supplierClazz.newInstance();
            List<String> includeProperties = null;
            if (mySupplier.get()) {
                includeProperties = Arrays.asList(viewProperty.includeProperties());
            }
            return includeProperties;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isAllowed(Field field) {

        boolean allowed = true;
        if (field.isAnnotationPresent(PropertyRolesAllowed.class)) {
            PropertyRolesAllowed rolesAllowed = field.getAnnotation(PropertyRolesAllowed.class);
            try {
                PropertyRolesAllowedAuthorizer propertyRolesAllowedAuthorizer = rolesAllowed.authorizer()
                        .newInstance();
                propertyRolesAllowedAuthorizer.authorize(rolesAllowed.value());
            } catch (Exception ex) {
                allowed = false;
            }
        }
        return allowed;
    }

    public static <E> E getFinalEntityPath(E entity, String pathToObject) throws Exception {

        String paths[] = pathToObject.split("\\.");
        for (String pathRef : paths) {
            if (entity instanceof Set) {
                String newPath = pathToObject.substring(pathToObject.indexOf("." + pathRef) + 1, pathToObject.length());
                Set persistentSet = (Set) entity;
                Set persistentSetExpanded = new HashSet();
                persistentSet.stream().forEach(ent -> {
                    try {
                        ent = getFinalEntityPath(ent, newPath);
                        persistentSetExpanded.add(ent);
                    } catch (Exception ex) {

                    }
                });
                return (E) persistentSetExpanded;

            } else {
                BeanInfo beanInfo = Introspector.getBeanInfo(entity.getClass());
                Optional<PropertyDescriptor> propertyDescriptorRef = Arrays.stream(beanInfo.getPropertyDescriptors())
                        .filter(propertyDescriptor -> propertyDescriptor.getName().equals(pathRef))
                        .findFirst();

                if (propertyDescriptorRef.isPresent()) {
                    entity = (E) invokeReadMethod(propertyDescriptorRef.get().getReadMethod(), entity);
                    if (entity == null) {
                        return null;
                    }
                } else {
                    logger.warn("Invalid pathReference in " + pathToObject + ": " + pathRef + " in class " + entity
                            .getClass());
                    return null;
                }
            }
        }
        return entity;
    }

    private static <E> void invokeSetMethod(BeanInfo beanInfo, View view, Field field, Object toSet,
            ViewProperty viewProperty, ContextView contextView) {

        try {
            if (beanInfo != null) {

                Method setMethod = ReflectionUtils.getWriteMethodForField(beanInfo, field);
                if (isFieldCollectionOfViews(field)) {
                    Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (toSet != null) {
                        List<Object> tempToSet = null;
                        if (!isSCollectionOfViews(toSet)) {
                            tempToSet = (List) ViewResolver
                                    .resolveFiltering(Arrays.asList(viewProperty.excludeProperties()),
                                            includePropertiesFilter(viewProperty), (Class) type, contextView,
                                            Sets.filter(new HashSet<>(Collection.class.cast(toSet)),
                                                            Predicates.and(getFilterForViewProperty(viewProperty),
                                                                    getContextFilterForViewProperty(viewProperty, contextView)))
                                                    .toArray());
                        } else {
                            tempToSet = new ArrayList<>(Collection.class.cast(toSet));
                        }
                        toSet = Optional.ofNullable(tempToSet).map(t -> toCollection(setMethod, t)).orElse(null);
                    }

                } else if (isFieldView(field)) {
                    toSet = ViewResolver.resolveEntityFiltering(Arrays.asList(viewProperty.excludeProperties()),
                            includePropertiesFilter(viewProperty), contextView,
                            (Class) field.getType(), toSet);
                }
                invokeSetMethod(setMethod, view, ReflectionUtils.adaptIfNecessary(toSet, setMethod));
            }
        } catch (LazyInitializationException ex) {

        } catch (Exception e) {
            logger.error("Error invokingSetMethod for field " + field.getName() + " " + e);
        }
    }

    private static boolean isSCollectionOfViews(Object toSet) {

        Set views = new HashSet<>(Collection.class.cast(toSet));
        return views.size() > 0 && views.stream().findFirst().get() instanceof View;
    }

    private static Collection toCollection(Method setMethod, List<Object> tempToSet) {

        if (setMethod.getParameterTypes()[0].isAssignableFrom(List.class)) {
            return tempToSet;
        } else {
            return new HashSet(tempToSet);
        }
    }

    private static Predicate getFilterForViewProperty(ViewProperty viewProperty) throws Exception {

        Predicate filter = new ViewProperty.NoFilter();
        if (viewProperty.filter() != null) {
            filter = viewProperty.filter().newInstance();
        }
        return filter;
    }

    private static Predicate getContextFilterForViewProperty(ViewProperty viewProperty,
            ContextView contextView) throws Exception {

        ContextFilterPredicate filter = new ViewProperty.NoFilterContext();
        if (viewProperty.filterContext() != null && contextView != null) {
            filter = viewProperty.filterContext().getDeclaredConstructor().newInstance();
            filter.applyContext(contextView);
        }
        return filter;
    }

    public static boolean isFieldView(Field field) {

        boolean isFieldView = false;
        if (View.class.isAssignableFrom(ReflectionUtils.findSuperClass(field.getType())) &&
                !ReflectionUtils.findSuperClass(field.getType())
                        .isAssignableFrom(Object.class)) {
            isFieldView = true;
        }
        return isFieldView;
    }

    public static boolean isFieldCollectionOfViews(Field field) {

        boolean isFieldCollectionOfViews = false;
        if (Collection.class.isAssignableFrom(field.getType())) {
            Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (View.class.isAssignableFrom(ReflectionUtils.findSuperClass((Class) type))) {
                isFieldCollectionOfViews = true;
            } else {
                isFieldCollectionOfViews = false;
            }
        }
        return isFieldCollectionOfViews;
    }

    private static void invokeSetMethod(Method writeMethod, View view, Object toSet) {

        try {
            writeMethod.invoke(view, toSet);
        } catch (Exception e) {

        }
    }

    private static <E> E invokeReadMethod(Method readMethod, E entity) {

        try {
            return (E) readMethod.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

}
