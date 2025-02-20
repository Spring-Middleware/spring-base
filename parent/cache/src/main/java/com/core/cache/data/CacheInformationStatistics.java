package com.core.cache.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheInformationStatistics implements CacheName {

    private String cacheName;
    private double hit;
    private double miss;
    private double pending;
    private double puts;

}
