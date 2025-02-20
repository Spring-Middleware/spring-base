package com.core.redis.service;

import com.core.converter.JsonConverter;
import com.core.redis.GetFunctions;
import com.core.redis.ToKeyValueFunction;
import com.core.redis.ToValueFunction;
import com.core.redis.data.ContainerInformationKey;
import com.core.redis.data.ContainerInformationValue;
import com.core.redis.data.DataValue;

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
