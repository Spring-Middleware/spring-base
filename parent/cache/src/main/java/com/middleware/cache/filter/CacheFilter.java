package com.middleware.cache.filter;

import com.middleware.cache.data.CacheName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.function.Predicate;

@Data
@NoArgsConstructor
public abstract class CacheFilter<N extends CacheName> implements Predicate<N> {

    public CacheFilter(Collection<String> cacheNames) {

        this.cacheNames = cacheNames;
    }

    private Collection<String> cacheNames;

    public boolean test(N cacheName) {

        return CollectionUtils.emptyIfNull(cacheNames).isEmpty() || cacheNames.contains(cacheName.getCacheName());
    }

}
