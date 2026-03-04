package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.manager.ProxyConfigurationClientManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupProxyConfigurator {

    private final ProxyConfigurationClientManager proxyConfigurationClientManager;

    public StartupProxyConfigurator(ProxyConfigurationClientManager proxyConfigurationClientManager) {
        this.proxyConfigurationClientManager = proxyConfigurationClientManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configureProxiesOnStartup() {
        proxyConfigurationClientManager.configureAll();
    }
}
