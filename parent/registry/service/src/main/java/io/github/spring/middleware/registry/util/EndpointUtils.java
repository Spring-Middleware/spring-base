package io.github.spring.middleware.registry.util;

public class EndpointUtils {

    private EndpointUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractHostPort(String endpoint) {
        endpoint = normalizeEndpoint(endpoint);
        int slash = endpoint.indexOf('/');
        return (slash >= 0) ? endpoint.substring(0, slash) : endpoint;
    }

    private static String normalizeEndpoint(String s) {
        if (s == null) return "";
        // normaliza barras dobles y trailing slash
        s = s.replaceAll("(?<!:)//+", "/"); // deja "http://"
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    // --- helpers de paths (evita // y soporta / opcional) ---
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String p = path.trim();
        if (!p.startsWith("/")) p = "/" + p;
        // no quitar trailing / aquí: depende de tu endpoint, pero suele dar igual en Spring
        return p;
    }

    public static String joinUrl(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) return base.substring(0, base.length() - 1) + path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

}
