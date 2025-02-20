package com.core.scope;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class RefreshableSingletonScopeRegister implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

        configurableListableBeanFactory.registerScope("refreshable-singleton", new RefresahbleSingletonOnDemandScope());
    }
}
