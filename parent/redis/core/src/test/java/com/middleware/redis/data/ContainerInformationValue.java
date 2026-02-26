package com.middleware.redis.data;

import com.middleware.redis.RedisKeyValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInformationValue implements RedisKeyValue<ContainerInformationKey, DataValue> {

    private ContainerInformationKey key;
    private DataValue dataValue;

    @Override
    public ContainerInformationKey getRedisKey() {

        return key;
    }

    @Override
    public DataValue getRedisValue() {

        return dataValue;
    }
}
