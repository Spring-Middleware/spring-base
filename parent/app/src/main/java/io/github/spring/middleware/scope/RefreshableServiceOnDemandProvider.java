package io.github.spring.middleware.scope;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

import java.util.Arrays;

public class RefreshableServiceOnDemandProvider<T> implements ObjectFactory<T> {

    private T instance;
    private ObjectFactory<T> creator;
    private static Logger log = Logger.getLogger(RefreshableServiceOnDemandProvider.class);

    public RefreshableServiceOnDemandProvider(ObjectFactory<T> creator) {

        this.creator = creator;
    }

    @Override
    public T getObject() throws BeansException {

        if (instance == null) {
            instance = creator.getObject();
            Arrays.stream(instance.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(InitMethod.class))
                    .forEach(m -> {
                        try {
                            m.invoke(instance);
                        } catch (Exception ex) {
                            log.error("Eror invoking init method on class " + instance.getClass());
                        }
                    });
        }
        return instance;
    }

    public void refresh() {

        this.instance = null;
    }
}
