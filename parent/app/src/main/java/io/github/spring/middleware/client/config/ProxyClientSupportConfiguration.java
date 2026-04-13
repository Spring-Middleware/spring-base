package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.proxy.StartupProxyConfigurator;
import io.github.spring.middleware.manager.ProxyConfigurationClientManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ProxyClientSupportConfiguration {

    @Bean
    public StartupProxyConfigurator startupProxyConfigurator(
            ObjectProvider<ProxyConfigurationClientManager> proxyConfigurationClientManagerProvider,
            ApplicationEventPublisher eventPublisher
    ) {
        ProxyConfigurationClientManager proxyConfigurationClientManager =
                proxyConfigurationClientManagerProvider.getIfAvailable();

        if (proxyConfigurationClientManager == null) {
            return null;
        }

        return new StartupProxyConfigurator(proxyConfigurationClientManager, eventPublisher);
    }
}
