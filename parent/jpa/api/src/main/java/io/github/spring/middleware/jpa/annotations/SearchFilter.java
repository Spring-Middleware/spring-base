package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.filter.FilterDB;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchFilter {

    Class<? extends FilterDB> filter() default NoFilter.class;

    class NoFilter implements FilterDB<Object, Object> {

        public void prepare(Collection<Object> entites, Object p) {
            //do nothing
        }

        public boolean apply(SearchFilter searchFilter, Object t, Object p) {

            return true;
        }
    }

}
