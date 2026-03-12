package io.github.spring.middleware.client.proxy.security.applier;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.client.proxy.security.config.SecurityApiKeyClientConfiguration;
import io.github.spring.middleware.client.proxy.security.method.ApiKeyMethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.method.MethodSecurityConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("API_KEY")
public class SecurityApiKeyApplier implements SecurityApplier<SecurityApiKeyClientConfiguration, ApiKeyMethodSecurityConfiguration> {

    @Override
    public WebClient.RequestHeadersSpec<?> applySecurity(
            SecurityApiKeyClientConfiguration securityConfiguration,
            ApiKeyMethodSecurityConfiguration methodSecurityConfiguration,
            Map<String, String> currentHeaders,
            WebClient.RequestHeadersSpec<?> specHeaders) {

        if (securityConfiguration.getHeaderName() == null || securityConfiguration.getHeaderName().isBlank()) {
            throw new ProxyClientException("headerName is required for API_KEY security configuration.");
        }

        if (methodSecurityConfiguration.key() == null || methodSecurityConfiguration.key().isBlank()) {
            throw new ProxyClientException("apiKeyValue is required for API_KEY security configuration.");
        }

        return specHeaders.header(
                securityConfiguration.getHeaderName(),
                methodSecurityConfiguration.key()
        );
    }

    @Override
    public boolean supports(MethodSecurityConfiguration methodSecurityConfiguration) {
        return methodSecurityConfiguration instanceof ApiKeyMethodSecurityConfiguration;
    }
}
