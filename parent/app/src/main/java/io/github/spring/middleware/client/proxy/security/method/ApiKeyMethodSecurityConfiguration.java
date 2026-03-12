package io.github.spring.middleware.client.proxy.security.method;

public record ApiKeyMethodSecurityConfiguration(String key) implements MethodSecurityConfiguration {

}
