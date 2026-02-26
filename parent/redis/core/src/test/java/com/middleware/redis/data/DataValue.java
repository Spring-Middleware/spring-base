package com.middleware.redis.data;

import com.middleware.redis.RedisValue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataValue implements RedisValue {

    private String value;
    private String name;


}
