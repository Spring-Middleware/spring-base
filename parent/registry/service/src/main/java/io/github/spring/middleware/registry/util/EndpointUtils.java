package io.github.spring.middleware.registry.util;

public class EndpointUtils {

    private EndpointUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractServiceBaseFromResource(String resourceEndpoint) {
        resourceEndpoint = normalizeEndpoint(resourceEndpoint);
        if (resourceEndpoint.isBlank()) {
            return "";
        }

        int firstSlash = resourceEndpoint.indexOf('/');
        if (firstSlash < 0) {
            return resourceEndpoint;
        }

        int secondSlash = resourceEndpoint.indexOf('/', firstSlash + 1);
        if (secondSlash < 0) {
            return resourceEndpoint;
        }

        return resourceEndpoint.substring(0, secondSlash);
    }

    public static String normalizeEndpoint(String s) {
        if (s == null) return "";
        s = s.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }


    public static String extractHostPort(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "";
        }

        int slash = endpoint.indexOf('/');
        return (slash >= 0) ? endpoint.substring(0, slash) : endpoint;
    }

    public static String normalizeResourcePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) return "";
        return normalizePath(path);
    }

    // --- helpers de paths (evita // y soporta / opcional) ---
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String p = path.trim();
        if (!p.startsWith("/")) p = "/" + p;
        // no quitar trailing / aquí: depende de tu endpoint, pero suele dar igual en Spring
        return p;
    }

    public static String normalizeContextPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) return "";
        String p = path.trim();
        if (!p.startsWith("/")) p = "/" + p;
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    public static String joinUrl(String base, String path) {
        if (path == null || path.isBlank()) return base;
        if (base.endsWith("/") && path.startsWith("/")) return base.substring(0, base.length() - 1) + path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

}
