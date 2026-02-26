package io.github.spring.middleware.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
public class CollectionBaseResponse<V> extends BaseResponse {

    private Collection<V> data;

}
