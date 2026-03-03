package io.github.spring.middleware.register.graphql;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "middleware.graphql")
public class GraphQLRegisterProperties {

    private String clusterName;
    private String namespace;
    private boolean enabled;

}
