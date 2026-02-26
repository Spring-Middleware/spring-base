package com.middleware.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleBaseResponse<V> extends BaseResponse {

    private V data;

}
