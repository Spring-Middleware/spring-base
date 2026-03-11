package io.github.spring.middleware.client.proxy.security.config;

import io.github.spring.middleware.client.proxy.security.SecurityClientType;
import lombok.Data;

@Data
public final class SecurityPassthroughClientConfiguration implements SecurityClientConfiguration {

    private String headerName;
    private boolean required;

    @Override
    public SecurityClientType getType() {
        return SecurityClientType.PASSTHROUGH;
    }
}
