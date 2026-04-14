package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.RegistryClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = "middleware.clients",
        name = "enabled",
        havingValue = "false"
)
public class RegistryClientNoOpAutoConfiguration {

    @Bean(name = "registryClient")
    public RegistryClient registryClient() {
        return new NoOpRegistryClient();
    }
}
