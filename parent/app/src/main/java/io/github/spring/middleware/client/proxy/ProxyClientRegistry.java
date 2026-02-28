package io.github.spring.middleware.client.proxy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class ProxyClientRegistry {

    private final static Set<ProxyClient<?>> proxyClients = new HashSet<>();

    public static void add(ProxyClient<?> client) {
        proxyClients.add(client);
    }

    public static Set<ProxyClient<?>> getAll() {
        return Collections.unmodifiableSet(proxyClients);
    }

}
