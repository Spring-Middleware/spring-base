package com.middleware.redis.service;

import com.middleware.converter.JsonConverter;
import com.middleware.redis.GetFunctions;
import com.middleware.redis.ToKeyValueFunction;
import com.middleware.redis.ToValueFunction;
import com.middleware.redis.data.ContainerInformationKey;
import com.middleware.redis.data.ContainerInformationValue;
import com.middleware.redis.data.DataValue;

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
