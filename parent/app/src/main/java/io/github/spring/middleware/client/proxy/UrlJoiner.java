package io.github.spring.middleware.client.proxy;

public class UrlJoiner {

    private UrlJoiner() {
    }

    public static String join(String base, String path) {
        if (base == null || base.isBlank()) throw new IllegalArgumentException("base is blank");
        if (path == null || path.isBlank()) return stripTrailingSlashes(base);

        String b = stripTrailingSlashes(base);
        String p = stripLeadingSlashes(path);

        return b + "/" + p;
    }

    private static String stripTrailingSlashes(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') end--;
        return s.substring(0, end);
    }

    private static String stripLeadingSlashes(String s) {
        int start = 0;
        while (start < s.length() && s.charAt(start) == '/') start++;
        return s.substring(start);
    }

}
