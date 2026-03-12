package io.github.spring.middleware.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sets default Actuator properties when the `app` module is present and the application
 * has not explicitly defined them. This ensures /actuator/health is exposed by default.
 */
public class ActuatorDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "app-actuator-defaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources sources = environment.getPropertySources();
        // Build defaults only for properties that are not present
        Map<String, Object> defaults = new LinkedHashMap<>();

        if (environment.getProperty("management.endpoints.web.exposure.include") == null) {
            defaults.put("management.endpoints.web.exposure.include", "health");
        }

        if (environment.getProperty("management.endpoint.health.enabled") == null) {
            defaults.put("management.endpoint.health.enabled", "true");
        }

        if (environment.getProperty("management.endpoints.web.base-path") == null) {
            // keep default /actuator unless user overrides, but set if missing to be explicit
            defaults.put("management.endpoints.web.base-path", "/actuator");
        }

        if (!defaults.isEmpty()) {
            MapPropertySource ps = new MapPropertySource(PROPERTY_SOURCE_NAME, defaults);
            sources.addLast(ps); // lowest precedence, user properties override
        }
    }

    @Override
    public int getOrder() {
        // run early but after system/environment are ready
        return Ordered.LOWEST_PRECEDENCE;
    }
}

