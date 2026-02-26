package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.redis.GetFunctions;
import io.github.spring.middleware.redis.ToKeyValueFunction;
import io.github.spring.middleware.redis.ToValueFunction;
import io.github.spring.middleware.redis.data.ContainerInformationKey;
import io.github.spring.middleware.redis.data.ContainerInformationValue;
import io.github.spring.middleware.redis.data.DataValue;

public class ContainerGetFunctions implements GetFunctions<ContainerInformationKey, DataValue, ContainerInformationValue> {

    private JsonConverter<DataValue> dataValueJsonConverter = new JsonConverter<>(DataValue.class);

    @Override
    public ToKeyValueFunction<ContainerInformationKey, DataValue, ContainerInformationValue> toKeyValueFunction() {

        return (k, v) -> {
            return ContainerInformationValue.builder()
                    .key(k)
                    .dataValue(v)
                    .build();
        };
    }

    @Override
    public ToValueFunction<ContainerInformationKey, DataValue> toValueFunction() {

        return (k, s) -> {
            DataValue dataValue = null;
            if (s != null) {
                dataValue = dataValueJsonConverter.toObject(s);
            }
            return dataValue;
        };
    }

}
