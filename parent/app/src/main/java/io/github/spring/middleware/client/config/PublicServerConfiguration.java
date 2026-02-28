package io.github.spring.middleware.client.config;

import io.github.spring.middleware.registry.model.PublicServer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "middleware.public-server")
public class PublicServerConfiguration {

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";

    private String host;
    private int port;
    private Boolean ssl;

    @Bean
    public PublicServer publicServer() {
        return new PublicServer(host, port, ssl);
    }
}
