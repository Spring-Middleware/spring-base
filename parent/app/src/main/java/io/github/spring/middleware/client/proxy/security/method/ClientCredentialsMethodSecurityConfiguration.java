package io.github.spring.middleware.client.proxy.security.method;

import java.util.List;

public record ClientCredentialsMethodSecurityConfiguration(
        List<String> requiredScopes) implements MethodSecurityConfiguration {

}
