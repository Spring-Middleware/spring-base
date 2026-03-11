package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.client.proxy.security.resolver.SecurityApiKeyHeaderResolver;
import io.github.spring.middleware.client.proxy.security.resolver.SecurityClientCredentialsHeaderResolver;
import io.github.spring.middleware.client.proxy.security.resolver.SecurityHeaderResolver;
import io.github.spring.middleware.client.proxy.security.resolver.SecurityNoneHeaderResolver;
import io.github.spring.middleware.client.proxy.security.resolver.SecurityPassthroughHeaderResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityHeaderResolverFactory {

    private final SecurityPassthroughHeaderResolver securityPassthroughHeaderResolver;
    private final SecurityClientCredentialsHeaderResolver securityClientCredentialsHeaderResolver;
    private final SecurityNoneHeaderResolver securityNoneHeaderResolver;
    private final SecurityApiKeyHeaderResolver securityApiKeyHeaderResolver;

    public SecurityHeaderResolver getInstance(SecurityClientType securityType) {
        return switch (securityType) {
            case NONE -> securityNoneHeaderResolver;
            case PASSTHROUGH -> securityPassthroughHeaderResolver;
            case API_KEY -> securityApiKeyHeaderResolver;
            case CLIENT_CREDENTIALS -> securityClientCredentialsHeaderResolver;
        };
    }

}
