package io.github.spring.middleware.cache.service;

import io.github.spring.middleware.cache.data.CacheInformationStatistics;
import io.github.spring.middleware.cache.data.RedisCacheConfigurationParameters;
import io.github.spring.middleware.cache.filter.CacheConfigurationFilter;
import io.github.spring.middleware.cache.filter.CacheInformationStadisticsFilter;

import java.util.Collection;

public interface CacheService {

    boolean clearCache(String cacheName);

    Collection<CacheInformationStatistics> getCacheInformationStadistics(
            CacheInformationStadisticsFilter stadisticsFilter);

    Collection<RedisCacheConfigurationParameters> getCacheConfigurations(CacheConfigurationFilter configurationFilter);

}
