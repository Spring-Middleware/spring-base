package com.core.jpa.query;

import com.core.jpa.adaptor.DataAdaptor;
import com.core.jpa.annotations.SearchProperties;
import com.core.jpa.annotations.SearchProperty;
import com.core.jpa.annotations.SubSearch;
import com.core.jpa.search.Search;
import com.core.jpa.utils.MethodInvoker;
import jakarta.persistence.Query;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryParameterizer<S extends Search> {

    private ParameterCounter parameterCounter;
    private Logger logger = Logger.getLogger(QueryParameterizer.class);

    public QueryParameterizer() {

        this.parameterCounter = new ParameterCounter();
    }

    public Query parameterizeQuery(Query query, S search) throws Exception {

        if (Optional.ofNullable(search).isPresent()) {
            Class<? super Search> clazzSearch = (Class) search.getClass();
            while (clazzSearch != null) {
                BeanInfo beanInfo = Introspector.getBeanInfo(clazzSearch);
                for (Field field : clazzSearch.getDeclaredFields()) {
                    if (field.isAnnotationPresent(SearchProperty.class)) {
                        SearchProperty searchProperty = field.getAnnotation(SearchProperty.class);
                        processSearchProperty(searchProperty, query, beanInfo, search, field);
                    } else if (field.isAnnotationPresent(SearchProperties.class)) {
                        SearchProperties searchProperties = field.getAnnotation(SearchProperties.class);
                        for (SearchProperty searchProperty : searchProperties.value()) {
                            processSearchProperty(searchProperty, query, beanInfo, search, field);
                        }
                    } else if (field.isAnnotationPresent(SubSearch.class)) {
                        S searchOr = (S) MethodInvoker.invokeReadMethod(beanInfo, search, field);
                        parameterizeQuery(query, searchOr);
                    }
                }
                clazzSearch = clazzSearch.getSuperclass();
            }
        }
        return query;
    }

    private void processSearchProperty(SearchProperty searchProperty, Query query, BeanInfo beanInfo, S search,
                                       Field field) {

        if (!searchProperty.isLike()) {
            processCommon(query, searchProperty, search, field, beanInfo);
        } else {
            processLike(query, search, field, beanInfo);
        }
        if (Optional.ofNullable(MethodInvoker.invokeReadMethod(beanInfo, search, field)).isPresent()) {
            processSearchPropertyParameters(searchProperty, query, search, null);
        }
    }

    private <S extends Search> void processLike(Query query, S search, Field field, BeanInfo beanInfo) {

        Optional.ofNullable(MethodInvoker.invokeReadMethod(beanInfo, search, field)).ifPresent(value -> {
            logger.debug(parameterCounter.actual() + " : " + value);
            query.setParameter(parameterCounter.next(), "%" + value.toString().toUpperCase() + "%");
        });
    }

    private <S extends Search> void processCommon(Query query, SearchProperty searchProperty, S search, Field field,
                                                  BeanInfo beanInfo) {

        Optional.ofNullable(MethodInvoker.invokeReadMethod(beanInfo, search, field))
                .filter(value -> Value.isValid(value)).ifPresent(value -> {
            if (value instanceof Collection) {
                processCollection(query, searchProperty, (Collection) value);
            } else {
                processValue(query, searchProperty, value);
            }
        });
    }

    private void processValue(Query query, SearchProperty searchProperty, Object value) {

        if (value instanceof Enum && !searchProperty.isEnum()) {
            value = ((Enum) value).name();
        }
        logger.debug(parameterCounter.actual() + " : " + value);
        query.setParameter(parameterCounter.next(), adapt(searchProperty, value));
    }

    private void processCollection(Query query, SearchProperty searchProperty, Collection collection) {

        if (collection.stream().anyMatch(e -> e instanceof Enum) && !searchProperty.isEnum()) {
            collection = (Collection) collection.stream().map(e -> Enum.class.cast(e).name())
                    .collect(Collectors.toList());
        }
        logger.debug(parameterCounter.actual() + " : " + collection);
        query.setParameter(parameterCounter.next(), collection);
    }

    private void processSearchPropertyParameters(SearchProperty searchProperty, Query query, S search,
                                                 Integer index) {

        Arrays.asList(searchProperty.parameters()).forEach(param -> {
            try {
                Function function = param.function().newInstance();
                String paramName = param.name();
                query.setParameter(paramName, function.apply(search));
                logger.debug(paramName + " : " + function.apply(search));
            } catch (Exception ex) {
                logger.error("Error setting searchProperty parameters " + searchProperty.value());
            }
        });
    }

    private <E, S> S adapt(SearchProperty searchProperty, E e) {

        try {
            DataAdaptor dataAdaptor = searchProperty.adaptor().newInstance();
            return (S) dataAdaptor.adapt(e);
        } catch (Exception ex) {
            logger.error("I can't adapt " + e + " with adaptor " + searchProperty.adaptor().getName(), ex);
            return (S) e;
        }
    }

}
