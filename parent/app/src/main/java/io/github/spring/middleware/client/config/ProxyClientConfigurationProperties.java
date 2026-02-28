package io.github.spring.middleware.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "middleware.client.proxy")
public class ProxyClientConfigurationProperties {

    private String registryEndpoint;
    private int maxRetries = 3;
    private long retryBackoffMillis = 1000;

}
