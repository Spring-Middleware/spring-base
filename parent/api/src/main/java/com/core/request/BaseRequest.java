package com.core.request;

import com.core.annotation.MappedContextParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseRequest {

    @MappedContextParam("REQUEST-ID")
    private String requestUUid;

}
