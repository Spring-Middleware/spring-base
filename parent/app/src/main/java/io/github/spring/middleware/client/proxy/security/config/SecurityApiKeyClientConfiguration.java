package io.github.spring.middleware.client.proxy.security.config;

import io.github.spring.middleware.client.proxy.security.SecurityClientType;
import lombok.Data;

@Data
public final class SecurityApiKeyClientConfiguration implements SecurityClientConfiguration {

    private String headerName;

    @Override
    public SecurityClientType getType() {
        return SecurityClientType.API_KEY;
    }
}
