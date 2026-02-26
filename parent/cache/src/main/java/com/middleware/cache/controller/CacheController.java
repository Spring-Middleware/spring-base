package com.middleware.cache.controller;

import com.middleware.cache.data.CacheInformationStatistics;
import com.middleware.cache.data.RedisCacheConfigurationParameters;
import com.middleware.cache.filter.CacheConfigurationFilter;
import com.middleware.cache.filter.CacheInformationStadisticsFilter;
import com.middleware.cache.service.CacheService;
import com.middleware.controller.CommonsController;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping(CacheController.BASE_MAPPING)
@OpenAPIDefinition(info = @Info(title = "Cache Controller"))
public class CacheController extends CommonsController {

    public static final String BASE_MAPPING = "/cache";

    @Autowired
    private CacheService cacheService;

    @GetMapping("/stadistics")
    public Collection<CacheInformationStatistics> getCacheInformationStadistics(
            @RequestParam(value = "cacheNames", required = false) Collection<String> cacheNames) {

        return cacheService.getCacheInformationStadistics(CacheInformationStadisticsFilter.builder()
                .cacheNames(cacheNames).build());
    }

    @GetMapping("/config")
    public Collection<RedisCacheConfigurationParameters> getCacheConfiguration(
            @RequestParam(value = "cacheNames", required = false) Collection<String> cacheNames) {

        return cacheService.getCacheConfigurations(CacheConfigurationFilter.builder()
                .cacheNames(cacheNames).build());
    }

    @GetMapping("/clear")
    public boolean clearCache(@PathParam("cacheName") String cacheName) {

        return cacheService.clearCache(cacheName);
    }

}
