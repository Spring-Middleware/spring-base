package com.middleware.redis.service;

import com.middleware.redis.data.UnitInformation;
import com.middleware.redis.data.UnitInformationKey;
import com.middleware.redis.service.unit.AbstractRedisUnitMerger;

import java.util.Collection;
import java.util.Optional;

public class UnitInformationMerger extends AbstractRedisUnitMerger<UnitInformationKey, UnitInformation> {

    @Override
    protected UnitInformation merge(UnitInformation current, UnitInformation next) {
        Optional.ofNullable(next.getText()).ifPresent(t -> current.setText(t));
        return current;
    }

    @Override
    protected boolean isContained(UnitInformationKey key, Collection<UnitInformationKey> keys) {
        return keys.stream().anyMatch(k -> k.equals(key));
    }
}
