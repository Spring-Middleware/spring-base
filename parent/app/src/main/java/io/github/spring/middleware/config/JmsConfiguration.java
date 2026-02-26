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

import java.util.List;

@Getter
@Setter
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "jms")
public class JmsConfiguration {

    private List<String> basePackages;
    private String profile;
    private String tcpHost;
    private String user;
    private String password;
    private Integer maxPool;
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

        if (basePackages != null) {
            JmsFactory jmsFactory = JmsFactory.newInstance();
            JmsConnectionConfiguration jmsConnectionConfiguration = getJmsConnectionConfiguration();
            basePackages.add("com.commons.jms");
            basePackages.add("com.commons.event");
            jmsResources = jmsFactory.createJmsResources(basePackages, jmsConnectionConfiguration);

        }
        return jmsResources;
    }

    private JmsConnectionConfiguration getJmsConnectionConfiguration() {

        JmsConnectionConfiguration jmsConnectionConfiguration = new JmsConnectionConfiguration();
        jmsConnectionConfiguration.setTcpHost(tcpHost);
        JmsConnectionCredentials jmsConnectionCredentials = new JmsConnectionCredentials();
        jmsConnectionCredentials.setUsername(user);
        jmsConnectionCredentials.setPassword(password);
        JmsConnectionPoolConfiguration jmsConnectionPoolConfiguration = new JmsConnectionPoolConfiguration();
        jmsConnectionPoolConfiguration.setMinIdle(minIdle);
        jmsConnectionPoolConfiguration.setMaxIdle(maxIdle);
        jmsConnectionPoolConfiguration.setMaxTotal(maxPool);
        jmsConnectionConfiguration.setJmsConnectionCredentials(jmsConnectionCredentials);
        jmsConnectionConfiguration.setJmsConnectionPoolConfiguration(jmsConnectionPoolConfiguration);
        return jmsConnectionConfiguration;
    }

}
