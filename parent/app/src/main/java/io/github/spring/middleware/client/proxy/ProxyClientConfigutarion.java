package io.github.spring.middleware.client.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "proxy-client")
public class ProxyClientConfigutarion {

    private long timeout = 5000;
}
