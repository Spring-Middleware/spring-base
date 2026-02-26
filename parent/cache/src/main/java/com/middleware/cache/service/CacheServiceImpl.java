package com.middleware.cache.service;

import com.middleware.cache.config.RedisCacheConfigurationSupport;
import com.middleware.cache.data.CacheInformationStatistics;
import com.middleware.cache.data.RedisCacheConfigurationParameters;
import com.middleware.cache.filter.CacheConfigurationFilter;
import com.middleware.cache.filter.CacheInformationStadisticsFilter;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private RedisCacheConfigurationSupport configurationSupport;

    private BeanInfo cacheBeanInfo = null;

    @PostConstruct
    public void init() throws Exception {

        cacheBeanInfo = Introspector.getBeanInfo(CacheInformationStatistics.class);
    }

    @Override
    public boolean clearCache(String cacheName) {

        String cacheNameFounded = cacheManager.getCacheNames().stream().filter(n -> n.equalsIgnoreCase(cacheName))
                .findFirst().orElse(null);
        if (cacheNameFounded != null) {
            cacheManager.getCache(cacheNameFounded).clear();
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    private CacheInformationStatistics initCache(String cacheName) {

        CacheInformationStatistics cacheInformationStatistics = new CacheInformationStatistics();
        cacheInformationStatistics.setCacheName(cacheName);
        return cacheInformationStatistics;
    }

    @Override
    public Collection<CacheInformationStatistics> getCacheInformationStadistics(
            CacheInformationStadisticsFilter stadisticsFilter) {

        Map<String, CacheInformationStatistics> stadisticsMap = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();

        meterRegistry.getMeters().forEach(meter -> {
            processMeter(cacheNames, meter, stadisticsMap);
        });
        return stadisticsMap.values().stream().filter(stadisticsFilter).collect(Collectors.toSet());
    }

    @Override
    public Collection<RedisCacheConfigurationParameters> getCacheConfigurations(
            CacheConfigurationFilter configurationFilter) {

        return configurationSupport.getRedisCacheConfigurationParameters().stream().filter(configurationFilter).collect(
                Collectors.toSet());
    }

    private void processMeter(Collection<String> cacheNames, Meter meter,
            Map<String, CacheInformationStatistics> stadisticsMap) {

        if (meter.getId().getName().equals("cache.gets")) {
            processCacheGets(cacheNames, meter, stadisticsMap);

        } else if (meter.getId().getName().equals("cache.puts")) {
            processCachePuts(cacheNames, meter, stadisticsMap);
        }
    }

    private void processCacheGets(Collection<String> cacheNames, Meter meter,
            Map<String, CacheInformationStatistics> stadisticsMap) {

        String cacheName = meter.getId().getTag("name");
        if (cacheNames.contains(cacheName)) {
            CacheInformationStatistics cacheInformationStatistics = stadisticsMap.compute(cacheName,
                    (k, v) -> v == null ? initCache(cacheName) : v);
            Measurement measurement = meter.measure().iterator().next();
            log.info("CacheName " + cacheName + " static " + meter.getId().getTag("result") + " : " +
                    measurement.getValue());
            Arrays.stream(cacheBeanInfo.getPropertyDescriptors())
                    .filter(pd -> pd.getName().equalsIgnoreCase(meter.getId().getTag("result")))
                    .findFirst().ifPresent(pd -> {
                        try {
                            pd.getWriteMethod().invoke(cacheInformationStatistics, measurement.getValue());
                        } catch (Exception ex) {
                            log.error("Error setting " + meter.getId().getTag("result" + " in cache " + cacheName));
                        }
                    });
        }
    }

    private void processCachePuts(Collection<String> cacheNames, Meter meter,
            Map<String, CacheInformationStatistics> stadisticsMap) {

        String cacheName = meter.getId().getTag("name");
        if (cacheNames.contains(cacheName)) {
            CacheInformationStatistics cacheInformationStatistics = stadisticsMap.compute(cacheName,
                    (k, v) -> v == null ? initCache(cacheName) : v);
            Measurement measurement = meter.measure().iterator().next();
            cacheInformationStatistics.setPuts(measurement.getValue());
        }
    }

}
