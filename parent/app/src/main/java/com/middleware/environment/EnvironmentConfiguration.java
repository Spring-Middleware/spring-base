package com.middleware.environment;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class EnvironmentConfiguration {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

}
