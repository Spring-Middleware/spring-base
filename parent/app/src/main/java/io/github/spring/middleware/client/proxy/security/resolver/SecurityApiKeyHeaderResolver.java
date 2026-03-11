package io.github.spring.middleware.client.proxy.security.resolver;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.client.proxy.security.config.SecurityApiKeyClientConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("API_KEY")
public class SecurityApiKeyHeaderResolver implements SecurityHeaderResolver<SecurityApiKeyClientConfiguration> {

    @Override
    public WebClient.RequestHeadersSpec<?> resolveSecurityHeader(
            SecurityApiKeyClientConfiguration securityConfiguration,
            Map<String, String> currentHeaders,
            WebClient.RequestHeadersSpec<?> specHeaders) {

        if (securityConfiguration.getHeaderName() == null || securityConfiguration.getHeaderName().isBlank()) {
            throw new ProxyClientException("headerName is required for API_KEY security configuration.");
        }

        if (securityConfiguration.getApiKeyValue() == null || securityConfiguration.getApiKeyValue().isBlank()) {
            throw new ProxyClientException("apiKeyValue is required for API_KEY security configuration.");
        }

        return specHeaders.header(
                securityConfiguration.getHeaderName(),
                securityConfiguration.getApiKeyValue()
        );
    }
}
