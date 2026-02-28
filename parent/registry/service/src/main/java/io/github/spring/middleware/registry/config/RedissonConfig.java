package io.github.spring.middleware.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
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
        // Create a custom ObjectMapper with JavaTimeModule to properly handle LocalDateTime
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Use JSON codec (Jackson) with the custom mapper so values in Redis are stored as JSON and dates are ISO strings
        config.setCodec(new JsonJacksonCodec(mapper));
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress(redissonConfigurationProperties.getAddress());
        if (redissonConfigurationProperties.getPassword() != null) {
            serverConfig.setPassword(redissonConfigurationProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
