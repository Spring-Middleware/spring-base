package io.github.spring.middleware.config;

import io.github.spring.middleware.jms.JmsActiveProfile;

import io.github.spring.middleware.rabbitmq.JmsFactory;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionConfiguration;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionCredentials;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionPoolConfiguration;
import io.github.spring.middleware.rabbitmq.core.JmsResources;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "middleware.jms")
public class JmsConfiguration {

    private List<String> basePackages = new ArrayList<>();
    private String profile;
    private String host;
    private String user;
    private String password;
    private Integer maxPoolSize;
    private Integer minIdle;
    private Integer maxIdle;
    private JmsResources jmsResources;

    @Bean(name = "JmsActiveProfile")
    public JmsActiveProfile createJmsActiveProfile() {

        JmsActiveProfile jmsActiveProfile = new JmsActiveProfile();
        jmsActiveProfile.setProfile(profile);
        return jmsActiveProfile;
    }

    @Bean
    @DependsOn({"JmsActiveProfile", "JmsActiveProfileSuffix","JmsResourceFactory"})
    public JmsResources configJms() throws Exception {
        JmsFactory jmsFactory = JmsFactory.newInstance();
        JmsConnectionConfiguration jmsConnectionConfiguration = getJmsConnectionConfiguration();
        basePackages.add("io.github.spring.middleware.jms");
        jmsResources = jmsFactory.createJmsResources(basePackages, jmsConnectionConfiguration);
        return jmsResources;
    }

    private JmsConnectionConfiguration getJmsConnectionConfiguration() {

        JmsConnectionConfiguration jmsConnectionConfiguration = new JmsConnectionConfiguration();
        jmsConnectionConfiguration.setTcpHost(host);
        JmsConnectionCredentials jmsConnectionCredentials = new JmsConnectionCredentials();
        jmsConnectionCredentials.setUsername(user);
        jmsConnectionCredentials.setPassword(password);
        JmsConnectionPoolConfiguration jmsConnectionPoolConfiguration = new JmsConnectionPoolConfiguration();
        jmsConnectionPoolConfiguration.setMinIdle(minIdle);
        jmsConnectionPoolConfiguration.setMaxIdle(maxIdle);
        jmsConnectionPoolConfiguration.setMaxTotal(maxPoolSize);
        jmsConnectionConfiguration.setJmsConnectionCredentials(jmsConnectionCredentials);
        jmsConnectionConfiguration.setJmsConnectionPoolConfiguration(jmsConnectionPoolConfiguration);
        return jmsConnectionConfiguration;
    }

}
