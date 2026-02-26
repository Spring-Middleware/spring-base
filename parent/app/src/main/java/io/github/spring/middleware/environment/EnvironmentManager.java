package io.github.spring.middleware.environment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "activeEnvironmentManager")
public class EnvironmentManager {

    private static EnvironmentConfiguration environmentConfiguration;

    @Autowired
    public void setEnvironmentConfiguration(EnvironmentConfiguration environmentConfiguration) {

        EnvironmentManager.environmentConfiguration = environmentConfiguration;
    }

    public static Environment getActiveEnvironment() {

        return Environment.getEnvironment(environmentConfiguration.getActiveProfile());
    }

}
