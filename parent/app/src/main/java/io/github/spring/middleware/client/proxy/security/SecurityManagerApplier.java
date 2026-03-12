package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import io.github.spring.middleware.client.proxy.security.method.MethodSecurityConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityManagerApplier {

    private final SecurityApplierFactory securityApplierFactory;

    public WebClient.RequestHeadersSpec<?> applySecurity(SecurityClientConfiguration securityConfiguration, MethodSecurityConfiguration methodSecurityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders) {
        var securityApplier = securityApplierFactory.getInstance(securityConfiguration.getType());
        if (securityApplier.supports(methodSecurityConfiguration)) {
            specHeaders = securityApplier.applySecurity(securityConfiguration, methodSecurityConfiguration, currentHeaders, specHeaders);
        } else {
            throw new ProxyClientException(STR."Security applier of type \{securityConfiguration.getType()} does not support method security configuration of type \{methodSecurityConfiguration.getClass().getSimpleName()}");
        }
        return specHeaders;
    }

}
