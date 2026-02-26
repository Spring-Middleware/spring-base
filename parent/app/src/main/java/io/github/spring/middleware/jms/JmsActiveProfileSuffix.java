package io.github.spring.middleware.jms;

import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationSuffix;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component(value = "JmsActiveProfileSuffix")
public class JmsActiveProfileSuffix implements DestinationSuffix, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public String version() {

        return applicationContext.getBean(JmsActiveProfile.class).getProfile();
    }

    @Override
    public void setApplicationContext(ApplicationContext myApplicationContext) throws BeansException {
        applicationContext = myApplicationContext;
    }
}
