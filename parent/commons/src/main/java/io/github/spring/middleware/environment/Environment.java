package io.github.spring.middleware.environment;

import java.util.Arrays;

public enum Environment {

    LOCAL,
    DEV,
    STAGE,
    PROD;

    public static Environment getEnvironment(String env) {

        return Arrays.stream(Environment.values()).filter(e -> e.toString().equalsIgnoreCase(env)).findFirst()
                .orElse(Environment.LOCAL);
    }

}
