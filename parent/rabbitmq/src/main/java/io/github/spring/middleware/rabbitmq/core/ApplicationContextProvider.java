package io.github.spring.middleware.rabbitmq.core;

import org.springframework.context.ApplicationContext;

public class ApplicationContextProvider {

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextProvider.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
