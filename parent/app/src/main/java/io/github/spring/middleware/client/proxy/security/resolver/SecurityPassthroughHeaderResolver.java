package io.github.spring.middleware.client.proxy.security.resolver;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.client.proxy.security.config.SecurityPassthroughClientConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("PASSTHROUGH")
public class SecurityPassthroughHeaderResolver
        implements SecurityHeaderResolver<SecurityPassthroughClientConfiguration> {

    @Override
    public WebClient.RequestHeadersSpec<?> resolveSecurityHeader(
            SecurityPassthroughClientConfiguration securityConfiguration,
            Map<String, String> currentHeaders,
            WebClient.RequestHeadersSpec<?> specHeaders) {

        String lookupHeaderName = securityConfiguration.getHeaderName().toLowerCase();
        String headerValue = currentHeaders.get(lookupHeaderName);

        if (headerValue == null && securityConfiguration.isRequired()) {
            throw new ProxyClientException(
                    STR."Required header \{securityConfiguration.getHeaderName()} is missing for PASSTHROUGH security configuration."
            );
        }

        if (headerValue != null) {
            return specHeaders.header(securityConfiguration.getHeaderName(), headerValue);
        }

        return specHeaders;
    }
}