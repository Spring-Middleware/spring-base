package io.github.spring.middleware.client.proxy;

public interface ClientConfigurable {

    String getClientName();

    void recreateHttpClient();

    void configureHttpClient();

}
