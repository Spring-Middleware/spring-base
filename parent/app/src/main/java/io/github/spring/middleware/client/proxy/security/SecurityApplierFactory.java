package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.client.proxy.security.applier.SecurityApiKeyApplier;
import io.github.spring.middleware.client.proxy.security.applier.SecurityClientCredentialsApplier;
import io.github.spring.middleware.client.proxy.security.applier.SecurityApplier;
import io.github.spring.middleware.client.proxy.security.applier.SecurityNoneApplier;
import io.github.spring.middleware.client.proxy.security.applier.SecurityPassthroughApplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityApplierFactory {

    private final SecurityPassthroughApplier securityPassthroughApplier;
    private final SecurityClientCredentialsApplier securityClientCredentialsApplier;
    private final SecurityNoneApplier securityNoneApplier;
    private final SecurityApiKeyApplier securityApiKeyApplier;

    public SecurityApplier getInstance(SecurityClientType securityType) {
        return switch (securityType) {
            case NONE -> securityNoneApplier;
            case PASSTHROUGH -> securityPassthroughApplier;
            case API_KEY -> securityApiKeyApplier;
            case CLIENT_CREDENTIALS -> securityClientCredentialsApplier;
        };
    }

}
