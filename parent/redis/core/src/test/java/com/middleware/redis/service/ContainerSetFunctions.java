package com.middleware.redis.service;

import com.middleware.converter.JsonConverter;
import com.middleware.redis.NewValueFunction;
import com.middleware.redis.SetFunctions;
import com.middleware.redis.ToKeyValueFunction;
import com.middleware.redis.ToStringValueFunction;
import com.middleware.redis.ToValueFunction;
import com.middleware.redis.UpdateValueFunction;
import com.middleware.redis.data.ContainerInformationKey;
import com.middleware.redis.data.ContainerInformationValue;
import com.middleware.redis.data.DataKeyValueRequest;
import com.middleware.redis.data.DataValue;

import java.util.Collection;

public class ContainerSetFunctions implements SetFunctions<ContainerInformationKey, DataValue, ContainerInformationValue> {

    private JsonConverter<DataValue> dataValueJsonConverter = new JsonConverter<>(DataValue.class);
    private Collection<DataKeyValueRequest> dataKeyValueRequests;

    public ContainerSetFunctions(Collection<DataKeyValueRequest> dataKeyValueRequests) {

        this.dataKeyValueRequests = dataKeyValueRequests;
    }

    @Override
    public ToStringValueFunction<DataValue> toStringValueFunction() {

        return v -> dataValueJsonConverter.toString(v);
    }

    @Override
    public ToValueFunction<ContainerInformationKey, DataValue> toValueFunction() {

        return (k, s) -> {
            DataValue dataValue = dataValueJsonConverter.toObject(s);
            return dataValue;
        };
    }

    @Override
    public UpdateValueFunction<ContainerInformationKey, DataValue> updateValueFunction() {

        return (k, v) -> {
            DataKeyValueRequest dataKeyValueRequest = dataKeyValueRequests.stream()
                    .filter(dkv -> dkv.getKey().equals(k.getKey())).findFirst().orElse(null);
            if (dataKeyValueRequest != null) {
                v.setName(dataKeyValueRequest.getName());
                v.setValue(dataKeyValueRequest.getValue());
            }
            return v;
        };
    }

    @Override
    public NewValueFunction<ContainerInformationKey, DataValue> newValueFunction() {

        return (k) -> {
            return new DataValue();
        };
    }

    @Override
    public ToKeyValueFunction<ContainerInformationKey, DataValue, ContainerInformationValue> toKeyValueFunction() {

        return (k, v) -> {
            return ContainerInformationValue.builder()
                    .key(k)
                    .dataValue(v).build();
        };
    }

}
