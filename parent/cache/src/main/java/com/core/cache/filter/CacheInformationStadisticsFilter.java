package com.core.cache.filter;

import com.core.cache.data.CacheInformationStatistics;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class CacheInformationStadisticsFilter extends CacheFilter<CacheInformationStatistics> {

    @Builder
    public CacheInformationStadisticsFilter(Collection<String> cacheNames) {

        super(cacheNames);
    }

    @Override
    public boolean test(CacheInformationStatistics cacheInformationStatistics) {

        return super.test(cacheInformationStatistics);
    }
}
