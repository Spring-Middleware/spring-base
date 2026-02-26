package com.middleware.redis.data;

import com.middleware.redis.RedisKey;
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
