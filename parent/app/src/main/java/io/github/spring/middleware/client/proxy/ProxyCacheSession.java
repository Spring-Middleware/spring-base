package io.github.spring.middleware.client.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProxyCacheSession {

    // ThreadLocal inicializa un HashMap vacío por hilo
    private static final ThreadLocal<Map<String, Object>> contextThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    // Reemplaza todo el mapa
    public static void set(Map<String, Object> myCache) {
        contextThreadLocal.set(myCache != null ? myCache : new HashMap<>());
    }

    // Devuelve el mapa actual, nunca nulo
    public static Map<String, Object> get() {
        return contextThreadLocal.get();
    }

    // Devuelve el valor de una clave concreta o null si no existe
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) contextThreadLocal.get().get(key);
    }

    // Pone un valor en la cache
    public static <T> void put(String key, T object) {
        if (key != null && object != null) {
            contextThreadLocal.get().put(key, object);
        }
    }

    // Limpia la cache del hilo actual
    public static void clear() {
        contextThreadLocal.get().clear();
    }

}
