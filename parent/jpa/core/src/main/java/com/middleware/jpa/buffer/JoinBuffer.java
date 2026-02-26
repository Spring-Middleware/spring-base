package com.middleware.jpa.buffer;

import com.middleware.jpa.annotations.Join;
import com.middleware.jpa.annotations.SearchProperty;
import com.middleware.jpa.search.Search;

import java.util.HashSet;
import java.util.Set;

public class JoinBuffer {

    private StringBuffer joinBuffer = new StringBuffer();
    private Set<String> joins = new HashSet<>();

    public void processJoinClass(Class<? super Search> clazzSearch) {

        if (clazzSearch.isAnnotationPresent(Join.class)) {
            Join join = clazzSearch.getAnnotation(Join.class);
            if (!joins.contains(join.value())) {
                appendJoin(join);
            }
        }
    }

    public void processJoinSearchProperty(Join join) {

        if (!join.value().isEmpty() && !joins.contains(join.value())) {
            appendJoin(join);
        }
    }

    private void processJoin(Join join) {

        if (!join.left() && !join.right()) {
            joinBuffer.append(" JOIN ");
        } else {
            if (join.left()) {
                joinBuffer.append(" LEFT JOIN ");
            } else if (join.right()) {
                joinBuffer.append(" RIGHT JOIN ");
            }
        }
        if (join.fetch()) {
            joinBuffer.append(" FETCH ");
        }
    }

    private void appendJoin(Join join) {

        processJoin(join);
        joinBuffer.append(join.value());
        joins.add(join.value());
    }

    public boolean isReferencedInJoin(String property) {

        String alias = property
                .substring(0, property.indexOf(".") > -1 ? property.indexOf(".") : property.length());
        alias = alias.replace("(", "");
        return joinBuffer.toString().contains(" " + alias + " ") || joinBuffer.toString().endsWith(" " + alias);
    }

    public boolean isReferencedInJoin(SearchProperty searchProperty) {

        return isReferencedInJoin(searchProperty.value().trim());
    }

    @Override
    public String toString() {

        return joinBuffer.toString();
    }
}
