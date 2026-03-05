package io.github.spring.middleware.config;

import io.github.spring.middleware.error.FrameworkErrorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FrameworkErrorProperties.class)
public class MiddlewareErrorAutoConfiguration {
}
