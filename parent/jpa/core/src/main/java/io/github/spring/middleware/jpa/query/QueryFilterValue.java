package io.github.spring.middleware.jpa.query;

import io.github.spring.middleware.jpa.filter.FilterDB;
import io.github.spring.middleware.jpa.annotations.SearchFilter;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

import java.util.Collection;

public class QueryFilterValue<F extends FilterDB, T> implements Predicate {

    private F filterDB;
    private Object value;
    private SearchFilter searchFilter;
    private Logger logger = Logger.getLogger(QueryFilterValue.class);

    public QueryFilterValue(F filterDB, SearchFilter searchFilter, Object value) {

        this.filterDB = filterDB;
        this.searchFilter = searchFilter;
        this.value = value;
    }

    public void prepareFilter(Collection<T> entities) {

        this.filterDB.prepare(entities, value);
    }

    public F getFilterDB() {

        return filterDB;
    }

    public Object getValue() {

        return value;
    }

    public boolean evaluate(Object entity) {

        try {
            return filterDB.apply(searchFilter, entity, value);
        } catch (Exception ex) {
            logger.error("Can't evaluate filter " + filterDB.getClass().getSimpleName() + " for entity " + entity +
                    " and value " + value);
            return false;
        }
    }
}
