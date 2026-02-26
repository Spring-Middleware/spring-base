package com.middleware.jpa.query;

import java.util.Collection;

public class Value {

    public static boolean isValid(Object value) {

        if (value != null) {
            if (value instanceof Collection) {
                return !((Collection) value).isEmpty();
            } else {
                return Boolean.TRUE;
            }
        } else {
            return Boolean.FALSE;
        }
    }

}
