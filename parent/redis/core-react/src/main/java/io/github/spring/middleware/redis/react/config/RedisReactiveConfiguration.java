package io.github.spring.middleware.redis.react.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveHashCommands;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "redis")
public class RedisReactiveConfiguration {

    private String host;
    private int port;
    private int database;

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        redisStandaloneConfiguration.setDatabase(database);
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public ReactiveKeyCommands keyCommands(ReactiveRedisConnectionFactory
                                                   reactiveRedisConnectionFactory) {

        return reactiveRedisConnectionFactory.getReactiveConnection().keyCommands();
    }

    @Bean
    public ReactiveStringCommands stringCommands(ReactiveRedisConnectionFactory
                                                         reactiveRedisConnectionFactory) {

        return reactiveRedisConnectionFactory.getReactiveConnection().stringCommands();
    }

    @Bean
    public ReactiveHashCommands hashCommands(RedisReactiveConfiguration redisReactiveConfiguration) {

        return reactiveRedisConnectionFactory().getReactiveConnection().hashCommands();
    }

}
