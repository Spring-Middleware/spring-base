package com.middleware.request;

import com.middleware.annotation.MappedContextParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseRequest {

    @MappedContextParam("REQUEST-ID")
    private String requestUUid;

}
