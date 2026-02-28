package io.github.spring.middleware.registry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "redisson")
public class RedissonConfigurationProperties {

    private String address;
    private String password;
    private int database;
}
