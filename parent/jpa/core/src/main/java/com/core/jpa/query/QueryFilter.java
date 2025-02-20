package com.core.jpa.query;

import com.core.jpa.filter.FilterDB;
import com.core.jpa.annotations.SearchFilter;
import com.core.jpa.search.Search;
import com.core.jpa.utils.MethodInvoker;
import org.apache.commons.collections.CollectionUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public class QueryFilter<T, S extends Search> {

    private final S search;
    private Collection<QueryFilterValue> filters = new HashSet<>();

    public QueryFilter(S search) throws Exception {

        this.search = search;
        buildFilters();
    }

    public void buildFilters() throws Exception {

        BeanInfo beanInfo = null;
        if (Optional.ofNullable(search).isPresent()) {
            Class<? super Search> clazzSearch = (Class) search.getClass();
            while (clazzSearch != null) {
                beanInfo = Introspector.getBeanInfo(clazzSearch);
                for (Field field : clazzSearch.getDeclaredFields()) {
                    Object value = MethodInvoker.invokeReadMethod(beanInfo, search, field);
                    if (field.isAnnotationPresent(SearchFilter.class) && value != null) {
                        SearchFilter searchFilter = field.getAnnotation(SearchFilter.class);
                        Class<? extends FilterDB> clazzFilter = (Class) searchFilter.filter();
                        QueryFilterValue queryFilterValue = new QueryFilterValue(clazzFilter.newInstance(),
                                searchFilter, value);
                        filters.add(queryFilterValue);
                    }
                }
                clazzSearch = clazzSearch.getSuperclass();
            }
        }
    }

    public boolean hasFilters() {

        return !filters.isEmpty();
    }

    public void applyFilters(Collection<T> enties) {

        filters.stream().forEach(queryFilterValue -> {
            queryFilterValue.prepareFilter(enties);
            CollectionUtils.filter(enties, queryFilterValue);
        });
    }

}
