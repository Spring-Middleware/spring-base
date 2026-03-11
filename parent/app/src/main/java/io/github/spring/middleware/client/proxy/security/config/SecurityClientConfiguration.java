package io.github.spring.middleware.client.proxy.security.config;

import io.github.spring.middleware.client.proxy.security.SecurityClientType;

public sealed interface SecurityClientConfiguration permits SecurityNoneClientConfiguration,
        SecurityPassthroughClientConfiguration,
        SecurityApiKeyClientConfiguration,
        SecurityClientCredentialsConfiguration {

    SecurityClientType getType();
}
