package com.core.scope;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RefresahbleSingletonOnDemandScope implements Scope {

    private Map<String, RefreshableServiceOnDemandProvider> providersMap
            = Collections.synchronizedMap(new HashMap<String, RefreshableServiceOnDemandProvider>());
    private Map<String, Runnable> destructionCallbacks
            = Collections.synchronizedMap(new HashMap<String, Runnable>());

    private ApplicationContext applicationContext;

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {

        if (!providersMap.containsKey(name)) {
            providersMap.put(name, new RefreshableServiceOnDemandProvider(objectFactory));
        }
        return providersMap.get(name).getObject();
    }

    @Override
    public Object remove(String name) {

        destructionCallbacks.remove(name);
        return Optional.ofNullable(providersMap.remove(name)).map(RefreshableServiceOnDemandProvider::getObject).orElse(null);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {

        destructionCallbacks.put(name, callback);
    }

    @Override
    public Object resolveContextualObject(String s) {

        return null;
    }

    @Override
    public String getConversationId() {

        return "refreshable-singleton";
    }

    public void refreshInstance(String name) {

        Optional.ofNullable(providersMap.get(name)).ifPresent(RefreshableServiceOnDemandProvider::refresh);
    }

}
