package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityHeaderApplier {

    private final SecurityHeaderResolverFactory securityHeaderResolverFactory;

    public  WebClient.RequestHeadersSpec<?>  applySecurityHeaders(SecurityClientConfiguration securityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders) {
        return securityHeaderResolverFactory.getInstance(securityConfiguration.getType()).resolveSecurityHeader(securityConfiguration, currentHeaders, specHeaders);
    }

}
