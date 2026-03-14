package io.github.spring.middleware.scope;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

public class RefreshableServiceOnDemandProvider<T> implements ObjectFactory<T> {

    private volatile T instance;
    private final ObjectFactory<T> creator;
    private static final Logger log = Logger.getLogger(RefreshableServiceOnDemandProvider.class);

    public RefreshableServiceOnDemandProvider(ObjectFactory<T> creator) {
        this.creator = creator;
    }

    @Override
    public synchronized T getObject() throws BeansException {
        if (instance == null) {
            instance = creator.getObject();
            invokeInitMethods(instance);
        }
        return instance;
    }

    public synchronized void refresh() {
        this.instance = null;
    }

    private void invokeInitMethods(T target) {
        if (target == null) {
            return;
        }

        Arrays.stream(target.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(InitMethod.class))
                .forEach(method -> invokeInitMethod(target, method));
    }

    private void invokeInitMethod(T target, Method method) {
        try {
            method.setAccessible(true);
            method.invoke(target);
        } catch (Exception ex) {
            log.error(STR."Error invoking init method '\{method.getName()}' on class \{target.getClass()}", ex);
        }
    }
}

