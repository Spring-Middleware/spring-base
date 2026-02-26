package io.github.spring.middleware.cache.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Getter
@Setter
@Builder
public class RedisCacheConfigurationParameters implements CacheName {

    private String cacheName;
    private int ttl;
    private ChronoUnit chronoUnit;

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RedisCacheConfigurationParameters that = (RedisCacheConfigurationParameters) o;
        return cacheName.equals(that.cacheName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(cacheName);
    }
}
