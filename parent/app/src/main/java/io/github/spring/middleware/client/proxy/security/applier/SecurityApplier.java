package io.github.spring.middleware.client.proxy.security.applier;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import io.github.spring.middleware.client.proxy.security.method.MethodSecurityConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public interface SecurityApplier<S extends SecurityClientConfiguration, M extends MethodSecurityConfiguration> {

    WebClient.RequestHeadersSpec<?> applySecurity(S securityConfiguration, M methodSecurityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders);

    boolean supports(MethodSecurityConfiguration methodSecurityConfiguration);
}

