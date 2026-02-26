package io.github.spring.middleware.redis.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DataKeyValueRequest {

    private String key;
    private String value;
    private String name;


}
