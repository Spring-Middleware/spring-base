package com.core.scope;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshableSingletonConfiguration {

    @Bean
    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {

        return new RefreshableSingletonScopeRegister();
    }
}
