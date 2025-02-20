package com.core.redis.data;

import com.core.redis.RedisKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContainerInformationKey implements RedisKey {

    private String key;

    @Builder
    public ContainerInformationKey(String key) {

        this.key = key;
    }
}
