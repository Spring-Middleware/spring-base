package io.github.spring.middleware.client.proxy.security.resolver;

import io.github.spring.middleware.client.proxy.security.config.SecurityNoneClientConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("NONE")
public class SecurityNoneHeaderResolver implements SecurityHeaderResolver<SecurityNoneClientConfiguration> {

    @Override
    public WebClient.RequestHeadersSpec<?> resolveSecurityHeader(SecurityNoneClientConfiguration securityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders) {
        return specHeaders;
    }
}
