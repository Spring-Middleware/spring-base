package io.github.spring.middleware.redis.data;

import io.github.spring.middleware.redis.unit.RedisUnitContainer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@Builder
public class ContainerInformation implements RedisUnitContainer<ContainerInformationKey, UnitInformation> {

    private ContainerInformationKey informationKey;
    private Collection<UnitInformation> unitInformations;

    @Override
    public ContainerInformationKey getRedisKey() {

        return informationKey;
    }

    @Override
    public Collection<UnitInformation> getRedisUnits() {

        return unitInformations;
    }
}
