package com.middleware.cache.service;

import com.middleware.cache.data.CacheInformationStatistics;
import com.middleware.cache.data.RedisCacheConfigurationParameters;
import com.middleware.cache.filter.CacheConfigurationFilter;
import com.middleware.cache.filter.CacheInformationStadisticsFilter;

import java.util.Collection;

public interface CacheService {

    boolean clearCache(String cacheName);

    Collection<CacheInformationStatistics> getCacheInformationStadistics(
            CacheInformationStadisticsFilter stadisticsFilter);

    Collection<RedisCacheConfigurationParameters> getCacheConfigurations(CacheConfigurationFilter configurationFilter);

}
