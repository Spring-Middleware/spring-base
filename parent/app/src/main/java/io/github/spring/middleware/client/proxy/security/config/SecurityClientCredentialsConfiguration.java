package io.github.spring.middleware.client.proxy.security.config;

import io.github.spring.middleware.client.proxy.security.SecurityClientType;
import lombok.Data;

import java.util.List;

@Data
public final class SecurityClientCredentialsConfiguration implements SecurityClientConfiguration {

    private String tokenUri;
    private String clientId;
    private String clientSecret;
    private List<String> scopes;

    @Override
    public SecurityClientType getType() {
        return SecurityClientType.CLIENT_CREDENTIALS;
    }
}
