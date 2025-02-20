package com.core.redis.data;

import com.core.redis.unit.RedisUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnitInformation implements RedisUnit<UnitInformationKey> {

    private UnitInformationKey key;
    private String text;

}
