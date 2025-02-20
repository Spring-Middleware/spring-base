package com.core.filter;

import java.util.Map;
import java.util.Optional;

public class Context {

    private static InheritableThreadLocal<Map<String, Object>> contextThreadLocal = new InheritableThreadLocal<>();

    public static void set(Map<String, Object> myContext) {

        contextThreadLocal.set(myContext);
    }

    public static <T> T get(String key) {

        return (T) Optional.ofNullable(contextThreadLocal.get()).map(m -> m.get(key)).orElse(null);
    }


    public static <T> void put(String key, T object) {

        contextThreadLocal.get().put(key, object);
    }
}
