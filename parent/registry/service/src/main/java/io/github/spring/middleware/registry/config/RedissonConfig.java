package io.github.spring.middleware.registry.config;

import lombok.AllArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AllArgsConstructor
@Configuration
public class RedissonConfig {

    private final RedissonConfigurationProperties redissonConfigurationProperties;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress(redissonConfigurationProperties.getAddress());
        if (redissonConfigurationProperties.getPassword() != null) {
            serverConfig.setPassword(redissonConfigurationProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
