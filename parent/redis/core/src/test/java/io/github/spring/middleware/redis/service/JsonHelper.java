package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.redis.data.ContainerInformation;
import io.github.spring.middleware.redis.data.UnitInformation;
import io.github.spring.middleware.redis.data.UnitInformationKey;

public class JsonHelper {

    private static JsonConverter<ContainerInformation> containerInformationJsonConverter = new JsonConverter<>(
            ContainerInformation.class);
    private static JsonConverter<UnitInformation> unitInformationJsonConverter = new JsonConverter<>(
            UnitInformation.class);
    private static JsonConverter<UnitInformationKey> unitInformationKeyJsonConverter = new JsonConverter<>(
            UnitInformationKey.class);

    public static ContainerInformation toContainerInformation(String json) {

        return containerInformationJsonConverter.toObject(json);
    }

    public static UnitInformation toUnitInformation(String json) {

        return unitInformationJsonConverter.toObject(json);
    }

    public static UnitInformationKey toUnitInformationKey(String json) {

        return unitInformationKeyJsonConverter.toObject(json);
    }

    public static String toJsonKey(UnitInformation unitInformation) {

        return unitInformationKeyJsonConverter.toString(unitInformation.getKey());
    }

    public static String toJson(UnitInformation unitInformation) {

        return unitInformationJsonConverter.toString(unitInformation);
    }

}
