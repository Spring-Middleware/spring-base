package io.github.spring.middleware.client.proxy.security.config;

import io.github.spring.middleware.client.proxy.security.SecurityClientType;

public final class SecurityNoneClientConfiguration implements SecurityClientConfiguration {

    @Override
    public SecurityClientType getType() {
        return SecurityClientType.NONE;
    }
}
