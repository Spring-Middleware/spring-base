package com.core.redis.service;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class RedisMutex<T> {

    private Map<T, AtomicReference<T>> mutexMap = Collections
            .synchronizedMap(new WeakHashMap<>());

    public AtomicReference<T> getMutex(T t) {

        return mutexMap.compute(t, (k, v) -> v == null ? new AtomicReference(k) : v);
    }

    public void removeMutex(T t) {

        mutexMap.remove(t);
    }

}
