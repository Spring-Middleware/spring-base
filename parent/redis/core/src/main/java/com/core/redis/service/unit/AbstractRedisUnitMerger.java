package com.core.redis.service.unit;

import com.core.redis.unit.RedisUnit;
import com.core.redis.unit.RedisUnitKey;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractRedisUnitMerger<UK extends RedisUnitKey, U extends RedisUnit<UK>> {

    public Collection<U> merge(Collection<U> currents, Collection<U> nexts) {

        Collection<U> mergeds = new HashSet<>();
        CollectionUtils.emptyIfNull(nexts).stream().forEach(next -> {
            if (isNew(next, currents)) {
                mergeds.add(next);
            } else {
                U current = currents.stream().filter(c -> c.getKey().equals(next.getKey())).findFirst()
                        .orElse(null);
                U merged = Optional.ofNullable(current).map(c -> merge(current, next)).orElse(null);
                if (merged != null) {
                    mergeds.add(merged);
                }
            }
        });
        mergeds.addAll(getCurrentsNotModified(currents, mergeds));
        return mergeds;
    }

    private boolean isNew(U unit, Collection<U> currents) {

        return unit.getKey() != null && !isContained(unit.getKey(),
                CollectionUtils.emptyIfNull(currents).stream().map(RedisUnit::getKey).collect(
                        Collectors.toSet()));
    }

    protected abstract U merge(U current, U next);

    private Collection<U> getCurrentsNotModified(Collection<U> currents,
                                                 Collection<U> nexts) {

        Set<UK> keys = CollectionUtils.emptyIfNull(nexts).stream().map(RedisUnit::getKey).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return CollectionUtils.emptyIfNull(currents).stream()
                .filter(current -> !isContained(current.getKey(), keys))
                .collect(Collectors.toSet());
    }

    protected abstract boolean isContained(UK key, Collection<UK> keys);

}
