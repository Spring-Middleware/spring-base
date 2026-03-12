package io.github.spring.middleware.client.proxy.security.applier;

import io.github.spring.middleware.client.proxy.security.config.SecurityNoneClientConfiguration;
import io.github.spring.middleware.client.proxy.security.method.MethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.method.VoidMethodSecurityConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("NONE")
public class SecurityNoneApplier implements SecurityApplier<SecurityNoneClientConfiguration, VoidMethodSecurityConfiguration> {

    @Override
    public WebClient.RequestHeadersSpec<?> applySecurity(SecurityNoneClientConfiguration securityConfiguration, VoidMethodSecurityConfiguration methodSecurityConfiguration, Map<String, String> currentHeaders, WebClient.RequestHeadersSpec<?> specHeaders) {
        return specHeaders;
    }

    @Override
    public boolean supports(MethodSecurityConfiguration methodSecurityConfiguration) {
        return methodSecurityConfiguration instanceof VoidMethodSecurityConfiguration;
    }
}
