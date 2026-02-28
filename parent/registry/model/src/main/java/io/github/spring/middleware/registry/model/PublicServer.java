package io.github.spring.middleware.registry.model;

public record PublicServer(
        String host,
        int port,
        Boolean ssl) {

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
}
