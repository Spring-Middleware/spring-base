package io.github.spring.middleware.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties
public class ProxyClientConfigurationTaskConfigurationProperties {

    private int retryDelaySeconds = 10;
    private int retryMinBackoffSeconds = 2;
    private int retryMaxBackoffSeconds = 60;
}
