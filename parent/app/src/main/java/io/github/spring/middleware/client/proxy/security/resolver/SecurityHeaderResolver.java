package io.github.spring.middleware.client.proxy.security.resolver;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public interface SecurityHeaderResolver<S extends SecurityClientConfiguration> {

    WebClient.RequestHeadersSpec<?> resolveSecurityHeader(S securityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders);
}

