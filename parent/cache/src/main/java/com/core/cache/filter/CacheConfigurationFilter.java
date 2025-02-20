package com.core.cache.filter;

import com.core.cache.data.RedisCacheConfigurationParameters;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class CacheConfigurationFilter extends CacheFilter<RedisCacheConfigurationParameters> {

    @Builder
    public CacheConfigurationFilter(Collection<String> cacheNames) {

        super(cacheNames);
    }

    @Override
    public boolean test(RedisCacheConfigurationParameters cacheInformationStadistics) {

        return super.test(cacheInformationStadistics);
    }

}
