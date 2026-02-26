package com.middleware.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchUtils {

    public static <T> Collection<T> searchByConditionsPriorities(Collection<T> collection,
            List<Predicate<T>> conditionsByPriority) {

        final AtomicReference<List<T>> reference = new AtomicReference<>();
        conditionsByPriority.forEach(condition -> {
            if (reference.get() == null || reference.get().isEmpty()) {
                reference.set(collection.stream().filter(s -> condition.test(s)).collect(Collectors.toList()));
            }
        });
        return reference.get();
    }

}
