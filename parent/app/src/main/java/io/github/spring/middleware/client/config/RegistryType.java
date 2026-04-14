package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.RegistryClient;

import java.lang.reflect.Proxy;

public enum RegistryType {

    NO_OP,
    REAL;

    public static RegistryType resolve(RegistryClient registryClient) {
        if (Proxy.isProxyClass(registryClient.getClass())) {
            return REAL;
        } else {
            return NO_OP;
        }
    }
}
