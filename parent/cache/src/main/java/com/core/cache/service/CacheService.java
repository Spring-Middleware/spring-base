package com.core.cache.service;

import com.core.cache.data.CacheInformationStatistics;
import com.core.cache.data.RedisCacheConfigurationParameters;
import com.core.cache.filter.CacheConfigurationFilter;
import com.core.cache.filter.CacheInformationStadisticsFilter;

import java.util.Collection;

public interface CacheService {

    boolean clearCache(String cacheName);

    Collection<CacheInformationStatistics> getCacheInformationStadistics(
            CacheInformationStadisticsFilter stadisticsFilter);

    Collection<RedisCacheConfigurationParameters> getCacheConfigurations(CacheConfigurationFilter configurationFilter);

}
