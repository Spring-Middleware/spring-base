package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.redis.data.ContainerInformation;
import io.github.spring.middleware.redis.data.ContainerInformationKey;
import io.github.spring.middleware.redis.data.UnitInformation;
import io.github.spring.middleware.redis.data.UnitInformationKey;
import io.github.spring.middleware.redis.service.unit.MappingAttributeFunction;
import io.github.spring.middleware.redis.component.MappingUnitFunction;
import io.github.spring.middleware.redis.service.unit.MergerUnitFunction;
import io.github.spring.middleware.redis.component.RedisKeyMapValues;
import io.github.spring.middleware.redis.component.RedisUnitContainerCreationFunction;

import java.util.Map;
import java.util.stream.Collectors;

public class RedisFunctions {

    private static UnitInformationMerger unitInformationMerger = new UnitInformationMerger();

    public static MappingUnitFunction<ContainerInformationKey, UnitInformationKey, UnitInformation> mappingUnitFunction() {

        return redisKeyMapValues -> {
            return redisKeyMapValues.getValues().entrySet().stream().map(entry -> {
                return JsonHelper.toUnitInformation(entry.getValue());
            }).collect(Collectors.toSet());
        };
    }

    public static MappingAttributeFunction<ContainerInformationKey, UnitInformationKey, UnitInformation> mappingAttributeFunction() {

        return (redisKey, units) -> {
            Map<String, String> attributes = units.stream()
                    .collect(Collectors.toMap(JsonHelper::toJsonKey, JsonHelper::toJson));
            return new RedisKeyMapValues<>(redisKey, attributes);
        };
    }

    public static MergerUnitFunction<UnitInformationKey, UnitInformation> mergerUnitFunction() {

        return ((currentUnits, nextUnits) -> {
            return unitInformationMerger.merge(currentUnits, nextUnits);
        });
    }

    public static RedisUnitContainerCreationFunction<ContainerInformationKey, UnitInformationKey, UnitInformation, ContainerInformation> redisUnitContainerCreationFunction() {

        return ((redisKey, units) -> {
            return ContainerInformation.builder()
                    .informationKey(ContainerInformationKey.builder().key(redisKey.getKey()).build())
                    .unitInformations(units)
                    .build();
        });
    }

}
