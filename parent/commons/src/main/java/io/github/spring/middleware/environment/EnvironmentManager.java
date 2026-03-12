package io.github.spring.middleware.environment;

import lombok.RequiredArgsConstructor;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnvironmentManager {

    private final EnvironmentConfiguration environmentConfiguration;

    public Environment getActiveEnvironment() {
        return Environment.getEnvironment(environmentConfiguration.getActiveProfile());
    }
}
