package com.middleware.jpa.buffer;

import com.middleware.jpa.annotations.SearchForClass;
import com.middleware.jpa.search.Search;

public class SelectBuffer {

    private StringBuffer selectBuffer = new StringBuffer();

    public void buildSelect(Search search, boolean isCount) {

        if (isCount) {
            countSelect(search);
        } else {
            querySelect(search);
        }
    }

    private void countSelect(Search search) {

        if (!isDistinct(search)) {
            selectBuffer.append("SELECT COUNT(c) ");
        } else {
            selectBuffer.append("SELECT COUNT(DISTINCT c) ");
        }
    }

    private void querySelect(Search search) {

        if (!isDistinct(search)) {
            selectBuffer.append("SELECT c ");
        } else {
            selectBuffer.append("SELECT DISTINCT c ");
        }
    }

    private boolean isDistinct(Search search) {

        if (search != null && search.getClass().isAnnotationPresent(SearchForClass.class)) {
            return search.getClass().getAnnotation(SearchForClass.class).distinct();
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public String toString() {

        return selectBuffer.toString();
    }
}
