package io.github.spring.middleware.register;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "middleware.resource-register")
public class ResourceRegisterConfiguration {

    private boolean enabled = true;
    private String clusterName;

}
